package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

/**
 * The modifier chip shown beside the resting dice: the banked consecutive-doubles +1 that lingers
 * until the next roll consumes it, or the Illusionist's Mirror Image -1 shown while the reroll it
 * penalized is the dice' last roll.
 */
@Immutable
sealed interface DiceBadge {
    @Immutable data object DoublesBonus : DiceBadge

    @Immutable data object MirrorPenalty : DiceBadge
}
