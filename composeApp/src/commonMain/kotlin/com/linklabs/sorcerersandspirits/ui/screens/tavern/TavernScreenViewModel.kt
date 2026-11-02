package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.computeropponent.ComputerOpponentIdentity
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassCopyUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.main.FirstScreenReadySignal
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TavernScreenViewModel(
    private val getTavernDataFlowUseCase: GetTavernDataFlowUseCase,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getCharacterClassCopyUseCase: GetCharacterClassCopyUseCase,
    private val computerOpponentIdentity: ComputerOpponentIdentity,
    private val firstScreenReadySignal: FirstScreenReadySignal,
) : ViewModel() {

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }
    private val classCopy by lazy { getCharacterClassCopyUseCase() }

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private var tavernDataJob: Job? = null

    private var lastChallengeAttempt: ChallengeAttempt? = null

    private var hasScheduledFirstScreenReady = false

    init {
        loadTavernData()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadTavernData()
    }

    fun retryLastChallenge() {
        val attempt = lastChallengeAttempt
        if (attempt != null) {
            onSendChallenge(
                userTraveler = attempt.userTraveler,
                selectedTravelerStates = attempt.selectedTravelerStates,
            )
        }
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        if (userTraveler.tavernStatus == TavernStatus.Busy) return

        lastChallengeAttempt =
            ChallengeAttempt(
                userTraveler = userTraveler,
                selectedTravelerStates = selectedTravelerStates,
            )

        viewModelScope.launch {
            val event =
                sendChallenges(
                    userTraveler = userTraveler,
                    selectedTravelerStates = selectedTravelerStates,
                )

            if (event != null) _eventChannelFlow.send(element = event)
        }
    }

    private suspend fun sendChallenges(
        userTraveler: TravelerState,
        selectedTravelerStates: Set<TravelerState>,
    ): TavernEvent? {
        val instigator =
            userTraveler.toChallenger(isInstigator = true, status = ChallengerStatus.Accepted)

        val invitees =
            selectedTravelerStates.map { travelerState ->
                travelerState.toChallenger(isInstigator = false, status = ChallengerStatus.Pending)
            }

        return when (
            val result = sendChallengeUseCase(instigator = instigator, invitees = invitees)
        ) {
            is SendChallengeResult.Success -> null

            is SendChallengeResult.NotEnoughChallengersAvailable ->
                TavernEvent.NotEnoughChallengersAvailable(
                    challengerStates = result.unavailableChallengers.map { it.toChallengerState() },
                )

            is SendChallengeResult.Failure -> TavernEvent.ShowChallengeSendFailed
        }
    }

    private fun loadTavernData() {
        tavernDataJob?.cancel()
        tavernDataJob =
            viewModelScope.launch {
                getTavernDataFlowUseCase()
                    .filterNotNull()
                    .catch {
                        _uiStateFlow.value = UiState.Error
                        firstScreenReadySignal.markReady()
                    }
                    .collect { data -> updateScreenState(data = data) }
            }
    }

    private fun updateScreenState(data: TavernData) {
        val tavernState =
            data.toState(characterClasses = characterClasses, classCopy = classCopy) ?: return

        _uiStateFlow.value = UiState.Loaded(data = tavernState.withComputerOpponent())
        signalFirstScreenReadyAfterSettle()
    }

    private fun TavernState.withComputerOpponent(): TavernState = copy(
        otherTravelers = listOf(computerOpponentTraveler()) + otherTravelers,
    )

    private fun computerOpponentTraveler(): TravelerState = TravelerState(
        userId = computerOpponentIdentity.userId,
        displayName = computerOpponentIdentity.displayName,
        isThisTravelerTheUser = false,
        tavernStatus = TavernStatus.Available,
        rating = 0,
        isComputerOpponent = true,
    )

    private fun signalFirstScreenReadyAfterSettle() {
        if (hasScheduledFirstScreenReady) return
        hasScheduledFirstScreenReady = true
        viewModelScope.launch {
            delay(timeMillis = TAVERN_CONTENT_FADE_MILLIS.toLong())
            firstScreenReadySignal.markReady()
        }
    }
}

internal const val TAVERN_CONTENT_FADE_MILLIS = 1000

private data class ChallengeAttempt(
    val userTraveler: TravelerState,
    val selectedTravelerStates: Set<TravelerState>,
)
