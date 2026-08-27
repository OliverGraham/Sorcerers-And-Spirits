package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DiceRollEntity {
    val doublesBonusEntity: DoublesBonusEntity?

    @Serializable
    @SerialName(value = ONE_DIE)
    data class OneDie(
        override val doublesBonusEntity: DoublesBonusEntity?,
        val wasDoublesBonusConsumed: Boolean,
        val value: Int,
        val diePosition: DiePosition,
        val isMirrorImageReroll: Boolean,
        val dieColor: DieColor = DieColor.NormalAttack,
        val disablesSibling: Boolean,
    ) : DiceRollEntity

    @Serializable
    @SerialName(value = TWO_DICE)
    data class TwoDice(
        override val doublesBonusEntity: DoublesBonusEntity?,
        val wasDoublesBonusConsumed: Boolean,
        val firstDie: Int,
        val secondDie: Int,
        val isMirrorImagePass: Boolean,
        val dieColor: DieColor = DieColor.NormalAttack,
    ) : DiceRollEntity

    companion object {
        private const val ONE_DIE = "OneDie"
        private const val TWO_DICE = "TwoDice"
    }
}

internal fun DiceRollEntity.toDomain(): DiceRoll = when (this) {
    is DiceRollEntity.OneDie ->
        DiceRoll.OneDie(
            value = value,
            diePosition = diePosition,
            doublesBonus = doublesBonusEntity?.toDomain(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
            isMirrorImageReroll = isMirrorImageReroll,
            dieColor = dieColor,
            disablesSibling = disablesSibling,
        )

    is DiceRollEntity.TwoDice ->
        DiceRoll.TwoDice(
            firstDie = firstDie,
            secondDie = secondDie,
            doublesBonus = doublesBonusEntity?.toDomain(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
            dieColor = dieColor,
            isMirrorImagePass = isMirrorImagePass,
        )
}

internal fun DiceRoll.toEntity(): DiceRollEntity = when (this) {
    is DiceRoll.OneDie ->
        DiceRollEntity.OneDie(
            value = value,
            diePosition = diePosition,
            doublesBonusEntity = doublesBonus?.toEntity(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
            isMirrorImageReroll = isMirrorImageReroll,
            dieColor = dieColor,
            disablesSibling = disablesSibling,
        )

    is DiceRoll.TwoDice ->
        DiceRollEntity.TwoDice(
            firstDie = firstDie,
            secondDie = secondDie,
            doublesBonusEntity = doublesBonus?.toEntity(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
            dieColor = dieColor,
            isMirrorImagePass = isMirrorImagePass,
        )
}
