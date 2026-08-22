package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

// TODO(SS-258): Is this now just an enum?
@Immutable
sealed interface ArenaAnimationState {
    data object None : ArenaAnimationState

    data object Flash : ArenaAnimationState

    data class Avatar(val isContinuous: Boolean = false) : ArenaAnimationState

    data class FlashAndAvatar(val isContinuous: Boolean = false) : ArenaAnimationState

    fun isFlash() = this == Flash || this is FlashAndAvatar

    fun isAvatar() = this is Avatar || this is FlashAndAvatar

    fun isAnimationContinuous() =
        when (this) {
            is Avatar -> isContinuous

            is FlashAndAvatar -> isContinuous

            is None,
            is Flash -> false
        }
}
