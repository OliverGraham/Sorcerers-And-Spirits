package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.UpdateArenaPlayerUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.getLoadedStateOrNull
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

// TODO Arena: As soon as possible, write tests for this behavior. DO NOT do like the other view
//  model test, with all the Firebase craziness. Figure out another way that makes more sense

@KoinViewModel
class ArenaScreenViewModel(
    @InjectedParam private val arguments: ArenaScreenViewModelArguments,
    private val chooseCharacterClassUseCase: ChooseCharacterClassUseCase,
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val updateArenaPlayerUseCase: UpdateArenaPlayerUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onTimerExpiration,
        )

    init {
        viewModelScope.launch {
            // TODO: Figure out better way for userId
            val userId = getUserIdUseCase()

            coordinateArenaMatchUseCase(
                    arenaId = arguments.arenaId,
                    expectedNumberOfPlayers = arguments.expectedNumberOfPlayers,
                )
                .collect { updatedArenaMatch ->
                    if (updatedArenaMatch == null) {
                        // TODO Arena: Probably show message briefly and then navigate back, with
                        //  cleanup, since if this is null then there was some problem
                        _uiStateFlow.value = UiState.Error
                        return@collect
                    }

                    val updatedArenaState =
                        getUpdatedState(
                            previousState = _uiStateFlow.getLoadedStateOrNull(),
                            updatedArenaMatch = updatedArenaMatch,
                            userId = userId,
                        )

                    _uiStateFlow.value = UiState.Loaded(data = updatedArenaState)
                }
        }
    }

    fun onChooseCharacterClass(playerState: PlayerState, characterClassState: CharacterClassState) {
        viewModelScope.launch {
            val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return@launch

            val nextPlayerId =
                loadedState.playerStatesMap.values
                    .filterNot { it == playerState }
                    .filter { it.characterClassState == null }
                    .randomOrNull()
                    ?.userId

            val arenaPlayer = playerState.copy(characterClassState = characterClassState).toDomain()

            chooseCharacterClassUseCase(
                arenaId = arguments.arenaId,
                arenaPlayer = arenaPlayer,
                nextPlayerId = nextPlayerId,
            )
        }
    }

    private fun getUpdatedState(
        previousState: ArenaState?,
        updatedArenaMatch: ArenaMatch,
        userId: String?,
    ): ArenaState {
        val timerAction =
            determineTimerAction(
                previousState = previousState,
                updatedArenaMatch = updatedArenaMatch,
            )

        return when (timerAction) {
            is TimerAction.Start -> {
                arenaTimer.startTimer(timerDuration = timerAction.timerDuration)
                updatedArenaMatch.toState(
                    secondsRemaining = timerAction.timerDuration,
                    userId = userId,
                )
            }

            is TimerAction.Stop -> {
                arenaTimer.stopTimer()
                updatedArenaMatch.toState(secondsRemaining = 0, userId = userId)
            }

            is TimerAction.None -> {
                updatedArenaMatch.toState(
                    secondsRemaining = previousState?.secondsRemaining ?: 0,
                    userId = userId,
                )
            }
        }
    }

    private fun determineTimerAction(
        previousState: ArenaState?,
        updatedArenaMatch: ArenaMatch,
    ): TimerAction {
        val previousPhase = previousState?.phase
        val currentPhase = updatedArenaMatch.phase

        return when (previousPhase to currentPhase) {
            null to ArenaPhase.WaitingForAllPlayers -> TimerAction.Start(timerDuration = 20)

            ArenaPhase.WaitingForAllPlayers to ArenaPhase.WaitingForAllPlayers -> TimerAction.None

            null to ArenaPhase.ChoosingClasses,
            ArenaPhase.WaitingForAllPlayers to ArenaPhase.ChoosingClasses ->
                TimerAction.Start(timerDuration = 20)

            ArenaPhase.ChoosingClasses to ArenaPhase.ChoosingClasses ->
                if (previousState?.activePlayer != updatedArenaMatch.activePlayer) {
                    TimerAction.Start(timerDuration = 20)
                } else {
                    TimerAction.None
                }

            ArenaPhase.ChoosingClasses to ArenaPhase.Combat -> {
                // TODO: Animation logic, start 15s turn timer
                TimerAction.Start(timerDuration = 20)
            }

            ArenaPhase.Combat to ArenaPhase.Combat -> {
                if (previousState?.activePlayer != updatedArenaMatch.activePlayer) {
                    // TODO: change timer duration based on type of roll (first roll choosing
                    //  opponent, or simply follow-up to opponent's health)
                    TimerAction.Start(timerDuration = 15)
                } else {
                    TimerAction.None
                }
            }

            ArenaPhase.Combat to ArenaPhase.Finished -> TimerAction.Stop

            else -> TimerAction.Stop
        }
    }

    private fun updateArenaStateSeconds(secondsRemaining: Int) {
        _uiStateFlow.updateLoaded { arenaState ->
            arenaState.copy(secondsRemaining = secondsRemaining)
        }
    }

    private suspend fun onTimerExpiration() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        when (loadedState.phase) {
            ArenaPhase.WaitingForAllPlayers -> {
                val playersJoined = loadedState.playerStatesMap.size
                if (playersJoined < arguments.expectedNumberOfPlayers) {
                    _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
                }
            }

            ArenaPhase.ChoosingClasses -> {
                // TODO
            }

            ArenaPhase.Combat -> {
                // TODO
            }

            ArenaPhase.Finished -> {
                // TODO
            }
        }
    }
}
