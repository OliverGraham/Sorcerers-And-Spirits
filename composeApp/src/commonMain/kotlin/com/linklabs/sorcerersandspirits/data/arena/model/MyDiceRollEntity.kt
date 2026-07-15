package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MyDiceRollEntity {

    @Serializable @SerialName(value = ONE_DIE) data class OneDie(val value: Int) : MyDiceRollEntity

    @Serializable
    @SerialName(value = TWO_DICE)
    data class TwoDice(val firstDie: Int, val secondDie: Int) : MyDiceRollEntity

    @Serializable
    @SerialName(value = REROLL)
    data class Reroll(
        val originalFirstDie: Int,
        val originalSecondDie: Int,
        val rerolledFirstDie: Int? = null,
        val rerolledSecondDie: Int? = null,
    ) : MyDiceRollEntity

    companion object {
        private const val ONE_DIE = "OneDie"
        private const val TWO_DICE = "TwoDice"
        private const val REROLL = "Reroll"
    }
}

internal fun MyDiceRollEntity.toDomain(): MyDiceRoll =
    when (this) {
        is MyDiceRollEntity.OneDie -> MyDiceRoll.OneDie(value = value)

        is MyDiceRollEntity.TwoDice ->
            MyDiceRoll.TwoDice(firstDie = firstDie, secondDie = secondDie)

        is MyDiceRollEntity.Reroll ->
            MyDiceRoll.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
            )
    }

internal fun MyDiceRoll.toEntity(): MyDiceRollEntity =
    when (this) {
        is MyDiceRoll.OneDie -> MyDiceRollEntity.OneDie(value = value)

        is MyDiceRoll.TwoDice ->
            MyDiceRollEntity.TwoDice(firstDie = firstDie, secondDie = secondDie)

        is MyDiceRoll.Reroll ->
            MyDiceRollEntity.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
            )
    }
