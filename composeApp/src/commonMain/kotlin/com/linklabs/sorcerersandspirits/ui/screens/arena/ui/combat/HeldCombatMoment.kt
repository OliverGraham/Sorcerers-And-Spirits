package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun <T> rememberHeldDuringSequence(liveValue: T, isSequencing: Boolean): T {
    var heldValue by remember { mutableStateOf(value = liveValue) }

    val displayedValue = if (isSequencing) heldValue else liveValue

    SideEffect { heldValue = displayedValue }

    return displayedValue
}
