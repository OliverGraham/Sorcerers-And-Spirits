package com.linklabs.sorcerersandspirits.ui.screens.tavern

sealed interface TavernEvent {
    data class NotEnoughChallengersAvailable(val challengerStates: List<ChallengerState>) : TavernEvent

    data object ShowChallengeSendFailed : TavernEvent
}
