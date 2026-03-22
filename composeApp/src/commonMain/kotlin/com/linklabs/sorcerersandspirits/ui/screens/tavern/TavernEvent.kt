package com.linklabs.sorcerersandspirits.ui.screens.tavern

sealed interface TavernEvent {
    data class NavigateToArena(val arenaId: String) : TavernEvent
}
