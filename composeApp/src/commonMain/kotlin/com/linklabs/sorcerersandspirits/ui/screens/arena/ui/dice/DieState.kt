package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState

// Before any roll lands the two dice rest on ones, as a normal attack would show them.
private const val DIE_RESTING_FACE = 1

data class DieState(
    val value: Int = DIE_RESTING_FACE,
    val isEnabled: Boolean = true,
    val tint: Color? = null,
    val badge: DiceBadgeState? = null,
    val settleGeneration: Int = 0,
    val isRollingLocally: Boolean = false,
)
