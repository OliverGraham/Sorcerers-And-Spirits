package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll

@Immutable
sealed interface MyDiceRollState {

    @Immutable data class OneDie(val value: Int) : MyDiceRollState

    @Immutable data class TwoDice(val firstDie: Int, val secondDie: Int) : MyDiceRollState

    @Immutable
    data class Reroll(
        val originalFirstDie: Int,
        val originalSecondDie: Int,
        val rerolledFirstDie: Int? = null,
        val rerolledSecondDie: Int? = null,
    ) : MyDiceRollState
}

internal fun MyDiceRoll.toState() =
    when (this) {
        is MyDiceRoll.OneDie -> MyDiceRollState.OneDie(value = value)

        is MyDiceRoll.TwoDice -> MyDiceRollState.TwoDice(firstDie = firstDie, secondDie = secondDie)

        is MyDiceRoll.Reroll ->
            MyDiceRollState.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
            )
    }

internal fun MyDiceRollState.toDomain() =
    when (this) {
        is MyDiceRollState.OneDie -> MyDiceRoll.OneDie(value = value)

        is MyDiceRollState.TwoDice -> MyDiceRoll.TwoDice(firstDie = firstDie, secondDie = secondDie)

        is MyDiceRollState.Reroll ->
            MyDiceRoll.Reroll(
                originalFirstDie = originalFirstDie,
                originalSecondDie = originalSecondDie,
                rerolledFirstDie = rerolledFirstDie,
                rerolledSecondDie = rerolledSecondDie,
            )
    }
