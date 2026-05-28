package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class DiceRollState(
    val attackDice: List<Int>,
    val doublesBonusDie: Int?,
    val rollType: RollTypeState,
)
