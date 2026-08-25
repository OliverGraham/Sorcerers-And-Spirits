package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.DoublesBonusState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.toState
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
private val diceRollStateIdCounter = AtomicInt(value = 0)

@OptIn(ExperimentalAtomicApi::class)
private fun nextDiceRollStateId(): Int = diceRollStateIdCounter.incrementAndFetch()

@Immutable
sealed interface DiceRollState {
    val doublesBonus: DoublesBonusState?
    val id: Int
    val diceBadge: DiceBadgeState?

    @Immutable
    data class OneDie(
        val value: Int,
        val diePosition: DiePosition,
        val dieColor: DieColor = DieColor.NormalAttack,
        val isSiblingDisabled: Boolean = true,
        val isMirrorImageReroll: Boolean = false,
        override val doublesBonus: DoublesBonusState? = null,
        val wasDoublesBonusConsumed: Boolean = false,
        override val id: Int = nextDiceRollStateId(),
    ) : DiceRollState {
        override val diceBadge =
            when {
                wasDoublesBonusConsumed -> DiceBadgeState.DoublesBonus(diePosition = diePosition)
                isMirrorImageReroll -> DiceBadgeState.MirrorImageBonus(diePosition = diePosition)
                else -> null
            }
    }

    @Immutable
    data class TwoDice(
        val firstDie: Int,
        val secondDie: Int,
        val dieColor: DieColor = DieColor.NormalAttack,
        override val doublesBonus: DoublesBonusState? = null,
        val wasDoublesBonusConsumed: Boolean = false,
        override val id: Int = nextDiceRollStateId(),
    ) : DiceRollState {
        override val diceBadge =
            if (wasDoublesBonusConsumed) {
                DiceBadgeState.DoublesBonus(diePosition = DiePosition.Second)
            } else {
                null
            }
    }
}

internal fun DiceRoll.toState(): DiceRollState = when (this) {
    is DiceRoll.OneDie ->
        DiceRollState.OneDie(
            value = value,
            diePosition = diePosition,
            dieColor = dieColor,
            isSiblingDisabled = disablesSibling,
            isMirrorImageReroll = isMirrorImageReroll,
            doublesBonus = doublesBonus?.toState(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
        )

    is DiceRoll.TwoDice ->
        DiceRollState.TwoDice(
            firstDie = firstDie,
            secondDie = secondDie,
            dieColor = dieColor,
            doublesBonus = doublesBonus?.toState(),
            wasDoublesBonusConsumed = wasDoublesBonusConsumed,
        )
}
