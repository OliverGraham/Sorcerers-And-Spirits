package com.linklabs.sorcerersandspirits.ui.screens.arena

// TODO Arena:
//  Use to do one-time animations, like when a phase changes or when an active player changes
//  What other events are needed?
sealed interface ArenaEvent {
    object MatchTimedOut : ArenaEvent
}
