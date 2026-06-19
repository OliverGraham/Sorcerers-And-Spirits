package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponents
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge.ChallengeHandler
import com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge.ChallengeUpdate
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
    private val updateChallengerStatusUseCase: UpdateChallengerStatusUseCase,
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
) : ViewModel() {

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val loadedUiState
        get() = (_uiStateFlow.value as? UiState.Loaded)?.data

    private val challengeHandler = initializeChallengeHandler()

    private var tavernDataJob: Job? = null

    init {
        loadTavernData()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadTavernData()
    }

    private fun loadTavernData() {
        tavernDataJob?.cancel()
        tavernDataJob =
            viewModelScope.launch {
                getTavernDataFlowUseCase()
                    .filterNotNull()
                    .catch { _uiStateFlow.value = UiState.Error }
                    .collect { data -> updateScreenState(data = data) }
            }
    }

    fun showChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = true)
    }

    fun hideChallengeDialog() {
        updateChallengeDialogVisibility(isVisible = false)
    }

    fun onSendChallenge(userTraveler: TravelerState, selectedTravelerStates: Set<TravelerState>) {
        val selectedComputers =
            selectedTravelerStates.filter { traveler ->
                ComputerOpponents.isComputer(userId = traveler.userId)
            }

        if (selectedComputers.isNotEmpty()) {
            startComputerMatch(computers = selectedComputers)
            return
        }

        viewModelScope.launch {
            challengeHandler.sendChallenges(
                userTraveler = userTraveler,
                selectedTravelerStates = selectedTravelerStates,
            )
        }
    }

    /**
     * Computer opponents have no Firebase presence to claim, so a computer match skips the human
     * challenge handshake and navigates straight to a freshly created arena seeded with the user
     * and the chosen computer opponents.
     */
    private fun startComputerMatch(computers: List<TravelerState>) {
        val computerOpponents =
            computers.map { traveler ->
                ComputerOpponent(userId = traveler.userId, displayName = traveler.displayName)
            }

        val arguments =
            ArenaScreenViewModelArguments(
                arenaId = generateComputerMatchId(),
                expectedNumberOfPlayers = computerOpponents.size + 1,
                computerOpponents = computerOpponents,
            )

        viewModelScope.launch {
            _eventChannelFlow.send(element = TavernEvent.NavigateToArena(arguments = arguments))
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun generateComputerMatchId(): String =
        "${ComputerOpponents.ID_PREFIX}match-" +
            "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(from = 0, until = 1_000_000)}"

    fun onAcceptChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Accepted)
    }

    fun onDeclineChallenge(challengeId: String) {
        updateChallengeStatus(challengeId = challengeId, status = ChallengerStatus.Declined)
    }

    private suspend fun updateScreenState(data: TavernData) {
        val challengeUpdate =
            challengeHandler.handleChallengeUpdate(
                previousChallengeState = loadedUiState?.challenge,
                updatedChallenge = data.challenge,
                userId = data.profile.userId,
            )

        when (challengeUpdate) {
            is ChallengeUpdate.ReadyToStart ->
                _eventChannelFlow.send(
                    element = TavernEvent.NavigateToArena(arguments = challengeUpdate.arguments)
                )

            is ChallengeUpdate.Ongoing -> {
                val tavernState =
                    data.toState(
                        challengeState = challengeUpdate.challengeState,
                        characterClasses = characterClasses,
                    ) ?: return

                _uiStateFlow.value = UiState.Loaded(data = tavernState)
            }
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
