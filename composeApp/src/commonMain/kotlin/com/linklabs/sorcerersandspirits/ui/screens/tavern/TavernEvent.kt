package com.linklabs.sorcerersandspirits.ui.screens.tavern

sealed interface TavernEvent {
    data class NavigateToArena(val arenaId: String) : TavernEvent

    data class NotEnoughChallengersAvailable(val challengerStates: List<ChallengerState>) :
        TavernEvent

    data object SendChallengeFailed : TavernEvent
}
