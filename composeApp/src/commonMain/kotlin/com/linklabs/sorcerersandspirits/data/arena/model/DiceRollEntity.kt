package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DiceRollEntity {
    val doublesBonusEntity: DoublesBonusEntity?

    @Serializable
    @SerialName(value = ONE_DIE)
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonusEntity: DoublesBonusEntity? = null,
    ) : DiceRollEntity

    @Serializable
    @SerialName(value = TWO_DICE)
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonusEntity: DoublesBonusEntity? = null,
    ) : DiceRollEntity

    companion object {
        private const val ONE_DIE = "OneDie"
        private const val TWO_DICE = "TwoDice"
    }
}

internal fun DiceRollEntity.toDomain(): DiceRoll =
    when (this) {
        is DiceRollEntity.OneDie ->
            DiceRoll.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonusEntity?.toDomain(),
            )

        is DiceRollEntity.TwoDice ->
            DiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonusEntity?.toDomain(),
            )
    }

internal fun DiceRoll.toEntity(): DiceRollEntity =
    when (this) {
        is DiceRoll.OneDie ->
            DiceRollEntity.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonusEntity = doublesBonus?.toEntity(),
            )

        is DiceRoll.TwoDice ->
            DiceRollEntity.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonusEntity = doublesBonus?.toEntity(),
            )
    }
