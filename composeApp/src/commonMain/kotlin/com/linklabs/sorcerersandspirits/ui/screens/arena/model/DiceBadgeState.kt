package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiePosition

@Immutable
sealed interface DiceBadgeState {
    val diePosition: DiePosition

    @Immutable
    data class DoublesBonus(override val diePosition: DiePosition = DiePosition.Second) :
        DiceBadgeState

    @Immutable data class MirrorPenalty(override val diePosition: DiePosition) : DiceBadgeState
}
