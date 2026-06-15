package com.linklabs.sorcerersandspirits.ui.screens.arena

// TODO(SS-013) Arena:
//  Use to do one-time animations, like when a phase changes or when an active player changes
//  What other events are needed?
sealed interface ArenaEvent {
    data object MatchTimedOut : ArenaEvent

    data object NavigateBack : ArenaEvent
}
