package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceRoll
import kotlinx.serialization.Serializable

@Serializable
data class DiceRollEntity(
    val attackDice: List<Int>,
    val doublesBonusDie: Int?,
    val rollTypeEntity: RollTypeEntity,
)

internal fun DiceRollEntity.toDomain(): DiceRoll =
    DiceRoll(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        rollType = rollTypeEntity.toDomain(),
    )

internal fun DiceRoll.toEntity(): DiceRollEntity =
    DiceRollEntity(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        rollTypeEntity = rollType.toEntity(),
    )
