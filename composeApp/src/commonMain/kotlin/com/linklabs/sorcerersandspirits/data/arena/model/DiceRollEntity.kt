package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceRoll
import kotlinx.serialization.Serializable

@Serializable
data class DiceRollEntity(
    val attackDice: List<Int>,
    val doublesBonusDie: Int?,
    val nextCombatActionEntity: NextCombatActionEntity,
)

internal fun DiceRollEntity.toDomain(): DiceRoll =
    DiceRoll(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        rollType = nextCombatActionEntity.toDomain(),
    )

internal fun DiceRoll.toEntity(): DiceRollEntity =
    DiceRollEntity(
        attackDice = attackDice,
        doublesBonusDie = doublesBonusDie,
        nextCombatActionEntity = rollType.toEntity(),
    )
