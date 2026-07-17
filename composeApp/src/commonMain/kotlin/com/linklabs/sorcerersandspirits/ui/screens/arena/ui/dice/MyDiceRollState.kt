package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll

@Immutable
sealed interface MyDiceRollState {
    val doublesBonus: DoublesBonus?

    @Immutable
    data class OneDie(val value: Int, override val doublesBonus: DoublesBonus?) : MyDiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonus?,
    ) : MyDiceRollState

    @Immutable
    data class Reroll(
        val originalFirstDie: Int,
        val originalSecondDie: Int,
        val rerolledFirstDie: Int? = null,
        val rerolledSecondDie: Int? = null,
        override val doublesBonus: DoublesBonus?,
    ) : MyDiceRollState
}

internal fun MyDiceRoll.toState() =
    when (this) {
        is MyDiceRoll.OneDie -> MyDiceRollState.OneDie(value = value, doublesBonus = doublesBonus)

        is MyDiceRoll.TwoDice ->
            MyDiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )

        is MyDiceRoll.Reroll ->
            MyDiceRollState.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
                doublesBonus = doublesBonus,
            )
    }

internal fun MyDiceRollState.toDomain() =
    when (this) {
        is MyDiceRollState.OneDie -> MyDiceRoll.OneDie(value = value, doublesBonus = doublesBonus)

        is MyDiceRollState.TwoDice ->
            MyDiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )

        is MyDiceRollState.Reroll ->
            MyDiceRoll.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
                doublesBonus = doublesBonus,
            )
    }
