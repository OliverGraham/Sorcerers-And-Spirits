package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments

sealed interface TavernEvent {
    data class NavigateToArena(val arguments: ArenaScreenViewModelArguments) : TavernEvent

    data class NotEnoughChallengersAvailable(val challengerStates: List<ChallengerState>) :
        TavernEvent

    data object ShowChallengeSendFailed : TavernEvent
}
