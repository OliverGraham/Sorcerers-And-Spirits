package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.PerformCombatActionUseCase
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toUpdatedState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.withSeconds
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.withSelectedClass
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
    private val performCombatActionUseCase: PerformCombatActionUseCase,
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
            val userId = getUserIdUseCase().orEmpty()

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

                    val updatedArenaStateUpdate =
                        updatedArenaMatch.toUpdatedState(
                            previousState = _uiStateFlow.getLoadedStateOrNull(),
                            userId = userId,
                        )

                    handleTimerAction(timerAction = updatedArenaStateUpdate.timerAction)

                    _uiStateFlow.value = UiState.Loaded(data = updatedArenaStateUpdate.nextState)
                }
        }
    }

    fun onSelectCharacterClass(characterClassState: CharacterClassState) {
        _uiStateFlow.updateLoaded { arenaState ->
            if (arenaState is ArenaState.ChoosingClasses) {
                arenaState.withSelectedClass(selectedClass = characterClassState)
            } else {
                arenaState
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

            _uiStateFlow.updateLoaded { arenaState ->
                if (arenaState is ArenaState.ChoosingClasses) {
                    arenaState.copy(selectedClass = null)
                } else {
                    arenaState
                }
            }
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

    fun onAttack(targetUserId: String) {
        _uiStateFlow.updateLoaded { arenaState ->
            if (arenaState is ArenaState.Combat && arenaState.isItThisUsersTurn) {
                // For simplified rules, we can't change targets once a kill roll is pending
                if (
                    arenaState.pendingRollType ==
                        com.linklabs.sorcerersandspirits.domain.arena.model.RollType.ArmorClass
                ) {
                    arenaState.copy(pendingTargetUserId = targetUserId)
                } else {
                    arenaState
                }
            } else {
                arenaState
            }
        }
    }

    fun onRollDice() {
        viewModelScope.launch {
            val loadedState =
                _uiStateFlow.getLoadedStateOrNull() as? ArenaState.Combat ?: return@launch
            if (!loadedState.isItThisUsersTurn) return@launch

            val targetUserId = loadedState.pendingTargetUserId ?: return@launch

            performCombatActionUseCase(
                arenaId = arguments.arenaId,
                attackerUserId = loadedState.userPlayer.userId,
                targetUserId = targetUserId,
                rollType = loadedState.pendingRollType,
            )
        }
    }

    private fun handleTimerAction(timerAction: TimerAction) {
        when (timerAction) {
            is TimerAction.Start -> arenaTimer.startTimer(timerDuration = timerAction.timerDuration)

            is TimerAction.Stop -> arenaTimer.stopTimer()

            is TimerAction.None -> {
                /* No changes needed */
            }
        }
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
