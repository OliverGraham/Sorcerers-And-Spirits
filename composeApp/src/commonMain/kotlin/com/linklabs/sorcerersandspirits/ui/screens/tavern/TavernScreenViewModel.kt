package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge.ChallengeHandler
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TavernScreenViewModel(
    private val getTavernDataFlowUseCase: GetTavernDataFlowUseCase,
    private val sendChallengeUseCase: SendChallengeUseCase,
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val loadedUiState
        get() = (_uiStateFlow.value as? UiState.Loaded)?.data

    private val challengeHandler = initializeChallengeHandler()

    init {
        viewModelScope.launch {
            getTavernDataFlowUseCase().filterNotNull().collect { data ->
                updateScreenState(data = data)
            }
        }
    }

    fun showChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = false)
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        viewModelScope.launch {
            challengeHandler.sendChallenges(
                userTraveler = userTraveler,
                selectedTravelerStates = selectedTravelerStates,
            )
        }
    }

    fun onAcceptChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Accepted)
    }

    fun onDeclineChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Declined)
    }

    private suspend fun updateScreenState(data: TavernData) {
        val challengeState =
            challengeHandler.handleChallengeUpdateAndGetState(
                previousChallengeState = loadedUiState?.challenge,
                updatedChallenge = data.challenge,
                userId = data.profile.userId,
            )

        if (challengeState?.navigateToArena == true) {
            val arguments =
                ArenaScreenViewModelArguments(
                    arenaId = challengeState.challengeId,
                    expectedNumberOfPlayers = challengeState.getExpectedNumberOfPlayers(),
                )
            _eventChannelFlow.send(element = TavernEvent.NavigateToArena(arguments = arguments))
        } else {
            val tavernState = data.toState(challengeState = challengeState) ?: return

            _uiStateFlow.value = UiState.Loaded(data = tavernState)
        }
    }

    private fun initializeChallengeHandler() =
        ChallengeHandler(
            challengeTimer =
                Timer(
                    coroutineScope = viewModelScope,
                    onTimerTick = ::updateChallengeStateSeconds,
                    onTimerExpiration = ::declineChallenge,
                ),
            sendChallengeUseCase = sendChallengeUseCase,
            updateChallengerStatusUseCase = updateChallengerStatusUseCase,
        )

    private fun declineChallenge() {
        val challengeId = loadedUiState?.challenge?.challengeId ?: return

        onDeclineChallenge(challengeId = challengeId)
    }

    private fun updateChallengeStatus(challengeId: String, status: ChallengerStatus) {
        viewModelScope.launch {
            challengeHandler.updateChallengerStatus(challengeId = challengeId, status = status)
            updateChallengeDialogVisibility(isVisible = false)
        }
    }

    private fun updateChallengeDialogVisibility(isVisible: Boolean) {
        _uiStateFlow.updateLoaded { tavernState ->
            val challenge = tavernState.challenge ?: return@updateLoaded tavernState

            tavernState.copy(challenge = challenge.copy(showChallengeDialog = isVisible))
        }
    }

    private fun updateChallengeStateSeconds(secondsRemaining: Int) {
        _uiStateFlow.updateLoaded { tavernState ->
            val challenge = tavernState.challenge ?: return@updateLoaded tavernState

            tavernState.copy(challenge = challenge.copy(secondsRemaining = secondsRemaining))
        }
    }
}
