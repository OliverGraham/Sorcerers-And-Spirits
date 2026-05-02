package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.UpdateArenaPlayerUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ActiveState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.withSeconds
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
            val loadedState = _uiStateFlow.getLoadedStateOrNull() as? ActiveState ?: return@launch

            val selectedArenaPlayer =
                ArenaPlayer.Selected(
                    userId = playerState.userId,
                    displayName = playerState.displayName,
                    characterClass = characterClassState.toDomain(),
                )
            val allPlayers = loadedState.otherPlayers.map { it.toDomain() } + selectedArenaPlayer

            chooseCharacterClassUseCase(
                arenaId = arguments.arenaId,
                selectedArenaPlayer = selectedArenaPlayer,
                allPlayers = allPlayers,
            )
        }
    }

    fun onIncomingMessageHandled() {
        _uiStateFlow.updateLoaded { arenaState ->
            if (arenaState.unhandledMessagesCount <= 0) return@updateLoaded arenaState

            val nextUnhandledCount = arenaState.unhandledMessagesCount - 1
            when (arenaState) {
                is ArenaState.WaitingForAllPlayers ->
                    arenaState.copy(unhandledMessagesCount = nextUnhandledCount)

                is ArenaState.ChoosingClasses ->
                    arenaState.copy(unhandledMessagesCount = nextUnhandledCount)

                is ArenaState.Combat -> arenaState.copy(unhandledMessagesCount = nextUnhandledCount)

                is ArenaState.Finished ->
                    arenaState.copy(unhandledMessagesCount = nextUnhandledCount)
            }
        }
    }

    private fun getUpdatedState(
        previousState: ArenaState?,
        updatedArenaMatch: ArenaMatch,
        userId: String?,
    ): ArenaState {
        val nextState =
            updatedArenaMatch.toState(
                previousUnhandledMessagesCount = previousState?.unhandledMessagesCount ?: 0,
                userId = userId,
                secondsRemaining = 0,
            )

        return when (
            val timerAction = determineTimerAction(previous = previousState, next = nextState)
        ) {
            is TimerAction.Start -> {
                arenaTimer.startTimer(timerDuration = timerAction.timerDuration)
                nextState.withSeconds(seconds = timerAction.timerDuration)
            }

            is TimerAction.Stop -> {
                arenaTimer.stopTimer()
                nextState.withSeconds(seconds = 0)
            }

            is TimerAction.None -> {
                nextState.withSeconds(seconds = previousState?.secondsRemaining ?: 0)
            }
        }
    }

    private fun determineTimerAction(previous: ArenaState?, next: ArenaState): TimerAction =
        when (next) {
            is ArenaState.WaitingForAllPlayers -> {
                if (previous == null) TimerAction.Start(timerDuration = 20) else TimerAction.None
            }

            is ArenaState.ChoosingClasses -> {
                // Restart timer only when first entering the selection phase
                // TODO: Animation logic, start 15s turn timer
                if (previous !is ArenaState.ChoosingClasses) {
                    TimerAction.Start(timerDuration = 15)
                } else {
                    TimerAction.None
                }
            }

            is ArenaState.Combat -> {
                // Check turn change using the shared 'Active' interface
                val turnChanged =
                    previous is ActiveState &&
                        previous.activePlayer.userId != next.activePlayer.userId
                // TODO: Turn changes based on roll type and abilities, etc.
                //  change timer duration based on type of roll (first roll choosing
                //  opponent, or simply follow-up to opponent's health)

                if (previous !is ArenaState.Combat || turnChanged) {
                    TimerAction.Start(timerDuration = 20)
                } else {
                    TimerAction.None
                }
            }

            is ArenaState.Finished -> TimerAction.Stop
        }

    private fun updateArenaStateSeconds(secondsRemaining: Int) {
        _uiStateFlow.updateLoaded { arenaState ->
            arenaState.withSeconds(seconds = secondsRemaining)
        }
    }

    private suspend fun onTimerExpiration() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        when (loadedState) {
            is ArenaState.WaitingForAllPlayers -> {
                _eventChannelFlow.send(element = ArenaEvent.MatchTimedOut)
            }

            is ArenaState.ChoosingClasses -> {
                // TODO
            }

            is ArenaState.Combat -> {
                // TODO
            }

            is ArenaState.Finished -> {
                // TODO
            }
        }
    }
}
