package com.linklabs.sorcerersandspirits.ui.screens.arena

sealed interface ArenaEvent {
    data object MatchTimedOut : ArenaEvent

    data object NavigateBack : ArenaEvent
}
