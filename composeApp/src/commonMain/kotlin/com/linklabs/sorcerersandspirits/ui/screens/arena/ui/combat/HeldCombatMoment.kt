package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Freezes [liveValue] at its last pre-sequence snapshot while [isSequencing] is true, then snaps to
 * live once the sequence settles. Combat seating, the active-roller set, and the seated players'
 * stats all ride this so the stage — arrangement and the numbers beside each avatar — updates only
 * after the animations and flash for a moment have played, never the instant the domain result
 * arrives.
 */
@Composable
internal fun <T> rememberHeldDuringSequence(liveValue: T, isSequencing: Boolean): T {
    var heldValue by remember { mutableStateOf(value = liveValue) }

    val displayedValue = if (isSequencing) heldValue else liveValue

    SideEffect { heldValue = displayedValue }

    return displayedValue
}
