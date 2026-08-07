package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponents
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassCopyUseCase
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.GetTavernDataFlowUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
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
    private val getCharacterClassesUseCase: GetCharacterClassesUseCase,
    private val getCharacterClassCopyUseCase: GetCharacterClassCopyUseCase,
) : ViewModel() {

    private val characterClasses by lazy { getCharacterClassesUseCase().map { it.toState() } }
    private val classCopy by lazy { getCharacterClassCopyUseCase() }

    private val _eventChannelFlow = Channel<TavernEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<TavernState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private var tavernDataJob: Job? = null

    init {
        loadTavernData()
    }

    fun retry() {
        _uiStateFlow.value = UiState.Loading
        loadTavernData()
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
                    challengerStates = result.unavailableChallengers.map { it.toChallengerState() }
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
                    .catch { _uiStateFlow.value = UiState.Error }
                    .collect { data -> updateScreenState(data = data) }
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

    private fun updateScreenState(data: TavernData) {
        val tavernState =
            data.toState(characterClasses = characterClasses, classCopy = classCopy) ?: return

        _uiStateFlow.value = UiState.Loaded(data = tavernState)
    }
}
