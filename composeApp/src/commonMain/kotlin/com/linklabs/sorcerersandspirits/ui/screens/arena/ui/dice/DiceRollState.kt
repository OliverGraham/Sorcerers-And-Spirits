package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toState

@Immutable
sealed interface DiceRollState {
    val doublesBonus: DoublesBonusState?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        val dieColor: DieColor = DieColor.NormalAttack,
        val isSiblingDisabled: Boolean = true,
        val isMirrorImageReroll: Boolean = false,
        override val doublesBonus: DoublesBonusState? = null,
    ) : DiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        val dieColor: DieColor = DieColor.NormalAttack,
        override val doublesBonus: DoublesBonusState? = null,
    ) : DiceRollState
}

internal fun DiceRollState.dieColorAt(diePosition: DiePosition): DieColor? =
    when (this) {
        is DiceRollState.OneDie ->
            when {
                diePosition == this.diePosition -> dieColor
                isSiblingDisabled -> DieColor.Disabled
                else -> null
            }

        is DiceRollState.TwoDice -> dieColor
    }

internal fun DiceRollState.rolledValueAt(diePosition: DiePosition): Int? =
    when (this) {
        is DiceRollState.OneDie -> value.takeIf { diePosition == this.diePosition }

        is DiceRollState.TwoDice ->
            when (diePosition) {
                DiePosition.First -> firstDie
                DiePosition.Second -> secondDie
            }
    }

internal fun DiceRoll.toState(): DiceRollState =
    when (this) {
        is DiceRoll.OneDie ->
            DiceRollState.OneDie(
                value = value,
                diePosition = diePosition,
                dieColor = dieColor,
                isSiblingDisabled = disablesSibling,
                isMirrorImageReroll = isMirrorImageReroll,
                doublesBonus = doublesBonus?.toState(),
            )

        is DiceRoll.TwoDice ->
            DiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                dieColor = dieColor,
                doublesBonus = doublesBonus?.toState(),
            )
    }
