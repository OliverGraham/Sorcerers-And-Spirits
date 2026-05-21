package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class DiceRollState(
    val attackDice: List<Int>,
    val doublesBonusDie: Int?,
    val rollType: RollTypeState,
)

internal fun DiceRoll.toState() =
    DiceRollState(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        rollType = rollType.toState(),
    )

internal fun DiceRollState.toDomain() =
    DiceRoll(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        rollType = rollType.toDomain(),
    )
