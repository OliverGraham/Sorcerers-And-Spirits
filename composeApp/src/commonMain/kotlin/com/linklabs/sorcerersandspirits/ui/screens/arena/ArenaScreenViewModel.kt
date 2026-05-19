package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.usecase.ChooseCharacterClassUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.PerformCombatActionUseCase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.UpdateArenaPlayerUseCase
import com.linklabs.sorcerersandspirits.domain.authentication.GetUserIdUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SetCurrentTravelerAvailableUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ActiveState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TimerAction
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toUpdatedState
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
    private val setCurrentTravelerAvailableUseCase: SetCurrentTravelerAvailableUseCase,
    private val updateArenaPlayerUseCase: UpdateArenaPlayerUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaScreenState>>(value = UiState.Loading)
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

                    val nextState = updatedArenaStateUpdate.nextState
                    _uiStateFlow.value = UiState.Loaded(data = nextState)

                    if (nextState.arenaState == ArenaState.Finished) {
                        // TODO Arena: Need to delete ArenaMatch in RTDB when it's over
                        setCurrentTravelerAvailableUseCase()
                        _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
                    }
                }
        }
    }

    fun onSelectCharacterClass(characterClassState: CharacterClassState) {
        _uiStateFlow.updateLoaded { screenState ->
            val arenaState =
                screenState.arenaState as? ArenaState.ChoosingClasses
                    ?: return@updateLoaded screenState

            screenState.copy(arenaState = arenaState.copy(selectedClass = characterClassState))
        }
    }

    fun onChooseCharacterClass(playerState: PlayerState, characterClassState: CharacterClassState) {
        viewModelScope.launch {
            val arenaState =
                _uiStateFlow.getLoadedStateOrNull()?.arenaState as? ActiveState ?: return@launch

            val selectedArenaPlayer =
                ArenaPlayer.Selected(
                    userId = playerState.userId,
                    displayName = playerState.displayName,
                    characterClass = characterClassState.toDomain(),
                )
            val allPlayers = arenaState.otherPlayers.map { it.toDomain() } + selectedArenaPlayer

            chooseCharacterClassUseCase(
                arenaId = arguments.arenaId,
                selectedArenaPlayer = selectedArenaPlayer,
                allPlayers = allPlayers,
            )

            _uiStateFlow.updateLoaded { screenState ->
                val nextArenaState =
                    if (screenState.arenaState is ArenaState.ChoosingClasses) {
                        screenState.arenaState.copy(selectedClass = null)
                    } else {
                        screenState.arenaState
                    }
                screenState.copy(arenaState = nextArenaState)
            }
        }
    }

    fun onIncomingMessageHandled() {
        _uiStateFlow.updateLoaded { screenState ->
            if (screenState.unhandledMessagesCount <= 0) return@updateLoaded screenState

            screenState.copy(unhandledMessagesCount = screenState.unhandledMessagesCount - 1)
        }
    }

    fun onAttack(targetUserId: String) {
        _uiStateFlow.updateLoaded { screenState ->
            val arenaState = screenState.arenaState
            if (arenaState is ArenaState.Combat && arenaState.isItThisUsersTurn) {
                // Once a target is chosen, it's locked until turn end or target defeated
                if (arenaState.pendingTargetUserId == null) {
                    screenState.copy(
                        arenaState = arenaState.copy(pendingTargetUserId = targetUserId)
                    )
                } else {
                    screenState
                }
            } else {
                screenState
            }
        }
    }

    fun onRollDice() {
        viewModelScope.launch {
            val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return@launch
            val arenaState = loadedState.arenaState as? ArenaState.Combat ?: return@launch
            if (!arenaState.isItThisUsersTurn) return@launch

            val targetUserId = arenaState.pendingTargetUserId ?: return@launch

            // TODO Arena: Clean this slop
            val playerMap =
                (arenaState.otherPlayers + arenaState.userPlayer).associate { playerState ->
                    playerState.userId to (playerState.toDomain() as ArenaPlayer.Selected)
                }

            performCombatActionUseCase(
                arenaId = arguments.arenaId,
                activePlayerUserId = arenaState.activePlayer.userId,
                playerMap = playerMap,
                attackerUserId = arenaState.userPlayer.userId,
                targetUserId = targetUserId,
                rollType = arenaState.pendingRollType,
                messageStartIndex = loadedState.messages.size,
            )
        }
    }

    fun onTryLeaveArena() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        if (loadedState.arenaState is ArenaState.Finished) {
            onConfirmLeaveArena()
        } else {
            _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = true) }
        }
    }

    // TODO Arena: Need a leave that updates stats with a forfeit, and one that triggers when game
    //  is finished

    fun onConfirmLeaveArena() {
        viewModelScope.launch {
            setCurrentTravelerAvailableUseCase()
            _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
            _eventChannelFlow.send(element = ArenaEvent.NavigateBack)
        }
    }

    fun onDismissForfeitDialog() {
        _uiStateFlow.updateLoaded { screenState -> screenState.copy(showForfeitDialog = false) }
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
        _uiStateFlow.updateLoaded { screenState ->
            screenState.copy(secondsRemaining = secondsRemaining)
        }
    }

    private suspend fun onTimerExpiration() {
        val loadedState = _uiStateFlow.getLoadedStateOrNull() ?: return

        when (loadedState.arenaState) {
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
