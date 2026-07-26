package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatMoment

@Composable
internal fun rememberHeldCombatMoment(
    liveMoment: CombatMoment,
    isSequencing: Boolean,
): CombatMoment {
    var heldMoment by remember { mutableStateOf(value = liveMoment) }

    val displayedMoment = if (isSequencing) heldMoment else liveMoment

    SideEffect { heldMoment = displayedMoment }

    return displayedMoment
}
