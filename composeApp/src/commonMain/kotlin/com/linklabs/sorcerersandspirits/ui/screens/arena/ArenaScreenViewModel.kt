package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CoordinateArenaMatchUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import com.linklabs.sorcerersandspirits.ui.uistate.getLoadedStateOrNull
import com.linklabs.sorcerersandspirits.ui.uistate.updateLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ArenaScreenViewModel(
    private val arguments: ArenaScreenViewModelArguments,
    private val coordinateArenaMatchUseCase: CoordinateArenaMatchUseCase,
) : ViewModel() {

    private val _eventChannelFlow = Channel<ArenaEvent>()
    val eventChannelFlow = _eventChannelFlow.receiveAsFlow()

    private val _uiStateFlow = MutableStateFlow<UiState<ArenaState>>(value = UiState.Loading)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val arenaTimer =
        Timer(
            coroutineScope = viewModelScope,
            onTimerTick = ::updateArenaStateSeconds,
            onTimerExpiration = ::onWaitingForPlayersTimerExpiration,
        )

    init {
        viewModelScope.launch {
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
                        )

                    _uiStateFlow.value = UiState.Loaded(data = updatedArenaState)
                }
        }
    }

    // TODO Arena:
    //  Previous null, new is WaitingForAllPlayers
    //      Start 20 second timer, everyone should connect by then
    //  Previous is WaitingForAllPlayers, new is WaitingForAllPlayers
    //      20 second timer still going, just a loading indicator
    //  Previous is WaitingForAllPlayers, new is ChoosingClasses
    //      Same as Previous is ChoosingClasses, new is ChoosingClasses
    //      Start a new 20 second timer
    //      Active player (given from RTDB) chooses their CharacterClass and updates RTDB, then
    //   Same as Previous is ChoosingClasses, new is ChoosingClasses
    //       Repeat previous until everyone has chosen their CharacterClass
    //  Previous is ChoosingClasses, new is Combat
    //      Start game, animation, whatever, then start 15 second timer for Active player
    //          to choose their target
    //      Active player rolls against a target, then updates RTDB
    //  Previous is Combat, new is Combat
    //      Active player could now roll against player's HP if applicable (with 5 second timer),
    //          or it goes to the next person
    //      And so on
    //  Previous is Combat, new is Finished
    //      etc.
    private fun getUpdatedState(
        previousState: ArenaState?,
        updatedArenaMatch: ArenaMatch,
    ): ArenaState {
        val previousPhase = previousState?.phase
        val currentPhase = updatedArenaMatch.phase
        val nextState = updatedArenaMatch.toState()

        // We use a Pair to represent the state transition clearly
        return when (previousPhase to currentPhase) {
            // Previous null, new is WaitingForAllPlayers
            null to ArenaPhase.WaitingForAllPlayers -> {
                // TODO: Start 20s timer
                nextState
            }
            ArenaPhase.WaitingForAllPlayers to ArenaPhase.WaitingForAllPlayers -> {
                // TODO: Keep 20s timer going, preserve seconds from previousState
                nextState.copy(secondsRemaining = previousState?.secondsRemaining ?: 0)
            }
            ArenaPhase.WaitingForAllPlayers to ArenaPhase.ChoosingClasses -> {
                // TODO: Start new 20s timer
                nextState
            }
            ArenaPhase.ChoosingClasses to ArenaPhase.ChoosingClasses -> {
                // TODO: If active player changed, restart 20s timer.
                // Else, preserve seconds from previousState.
                nextState.copy(secondsRemaining = previousState?.secondsRemaining ?: 0)
            }
            ArenaPhase.ChoosingClasses to ArenaPhase.Combat -> {
                // TODO: Animation logic, start 15s turn timer
                nextState
            }
            ArenaPhase.Combat to ArenaPhase.Combat -> {
                // TODO: If turn changed (new active player), start 5s or 15s timer.
                // Else, preserve seconds from previousState.
                nextState.copy(secondsRemaining = previousState?.secondsRemaining ?: 0)
            }
            ArenaPhase.Combat to ArenaPhase.Finished -> {
                // TODO: Stop all timers, cleanup
                nextState
            }
            else -> {
                nextState.copy(secondsRemaining = previousState?.secondsRemaining ?: 0)
            }
        }
    }

    private fun updateArenaStateSeconds(secondsRemaining: Int) {
        _uiStateFlow.updateLoaded { arenaState ->
            arenaState.copy(secondsRemaining = secondsRemaining)
        }
    }

    private suspend fun onWaitingForPlayersTimerExpiration() {
        val loadedState = _uiStateFlow.value as? UiState.Loaded ?: return

        when (loadedState.data.phase) {
            ArenaPhase.WaitingForAllPlayers -> {
                val playersJoined = loadedState.data.playerStatesMap.size
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
