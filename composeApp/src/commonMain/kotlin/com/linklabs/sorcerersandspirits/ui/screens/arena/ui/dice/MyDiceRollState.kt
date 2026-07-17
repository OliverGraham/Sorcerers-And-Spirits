package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll
import com.linklabs.sorcerersandspirits.domain.game.DiePosition

@Immutable
sealed interface MyDiceRollState {
    val doublesBonus: DoublesBonus?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonus: DoublesBonus?,
    ) : MyDiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonus?,
    ) : MyDiceRollState
}

internal fun MyDiceRoll.toState() =
    when (this) {
        is MyDiceRoll.OneDie ->
            MyDiceRollState.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonus,
            )

        is MyDiceRoll.TwoDice ->
            MyDiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )
    }

internal fun MyDiceRollState.toDomain() =
    when (this) {
        is MyDiceRollState.OneDie ->
            MyDiceRoll.OneDie(value = value, diePosition = diePosition, doublesBonus = doublesBonus)

        is MyDiceRollState.TwoDice ->
            MyDiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus,
            )
    }
