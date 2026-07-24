package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toState

@Immutable
sealed interface DiceRollState {
    val doublesBonus: DoublesBonusState?
    val disabledDiePosition: DiePosition?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        val tint: Color? = null,
        val isMirrorImageReroll: Boolean = false,
        override val doublesBonus: DoublesBonusState? = null,
        override val disabledDiePosition: DiePosition? = null,
    ) : DiceRollState

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        override val doublesBonus: DoublesBonusState? = null,
    ) : DiceRollState {
        override val disabledDiePosition: DiePosition? = null
    }
}

internal fun DiceRollState.tintAt(diePosition: DiePosition): Color? =
    when (this) {
        is DiceRollState.OneDie -> tint.takeIf { diePosition == this.diePosition }
        is DiceRollState.TwoDice -> null
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

internal fun DiceRoll.toState(playerMap: Map<String, PlayerState.Selected>): DiceRollState =
    when (this) {
        is DiceRoll.OneDie -> {
            val isRollOff = rollOffOwnerUserId != null
            DiceRollState.OneDie(
                value = value,
                diePosition = diePosition,
                isMirrorImageReroll = isMirrorImageReroll,
                doublesBonus = doublesBonus?.toState(),
                tint = playerMap[rollOffOwnerUserId]?.characterClass?.type?.toColor(),
                disabledDiePosition = if (isRollOff) null else diePosition.opposite(),
            )
        }

        is DiceRoll.TwoDice ->
            DiceRollState.TwoDice(
                firstDie = firstDie,
                secondDie = secondDie,
                doublesBonus = doublesBonus?.toState(),
            )
    }

private fun DiePosition.opposite(): DiePosition =
    when (this) {
        DiePosition.First -> DiePosition.Second
        DiePosition.Second -> DiePosition.First
    }
