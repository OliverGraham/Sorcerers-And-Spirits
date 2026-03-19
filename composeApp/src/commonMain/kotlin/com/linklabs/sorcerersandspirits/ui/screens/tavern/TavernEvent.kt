package com.linklabs.sorcerersandspirits.ui.screens.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge

sealed interface TavernEvent {
    data class ChallengeReceived(val challenge: ArenaChallenge) : TavernEvent
    data class NavigateToArena(val arenaId: String) : TavernEvent
}
