package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ArenaAnimationState {
    data object None : ArenaAnimationState

    data object Flash : ArenaAnimationState

    data object Avatar : ArenaAnimationState

    data object FlashAndAvatar : ArenaAnimationState
}
