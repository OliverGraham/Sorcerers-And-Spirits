package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toDomain
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toState

@Immutable
sealed interface MyDiceRollState {
    val doublesBonus: DoublesBonusState?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        override val doublesBonus: DoublesBonusState?,
    ) : MyDiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonusState?,
    ) : MyDiceRollState
}

internal fun MyDiceRoll.toState() =
    when (this) {
        is MyDiceRoll.OneDie ->
            MyDiceRollState.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonus?.toState(),
            )

        is MyDiceRoll.TwoDice ->
            MyDiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus?.toState(),
            )
    }

internal fun MyDiceRollState.toDomain() =
    when (this) {
        is MyDiceRollState.OneDie ->
            MyDiceRoll.OneDie(
                value = value,
                diePosition = diePosition,
                doublesBonus = doublesBonus?.toDomain(),
            )

        is MyDiceRollState.TwoDice ->
            MyDiceRoll.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus?.toDomain(),
            )
    }
