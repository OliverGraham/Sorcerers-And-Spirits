package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction

data class DiceRoll(
    val attackDice: List<Int>,
    val doublesBonusDie: Int?,
    val rollType: NextCombatAction,
)
