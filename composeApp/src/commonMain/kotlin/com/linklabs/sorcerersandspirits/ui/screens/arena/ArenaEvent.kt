package com.linklabs.sorcerersandspirits.ui.screens.arena

// TODO Arena: What events are needed?
sealed interface ArenaEvent {
    object MatchTimedOut : ArenaEvent
}
