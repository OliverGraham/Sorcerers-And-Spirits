package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState

@Stable
internal class DiceStateHolder {
    var isHolding by mutableStateOf(value = false)
        private set

    var isArmed by mutableStateOf(value = false)
        private set

    var shakeTick by mutableStateOf(value = 0)
        private set

    var isUserRolling by mutableStateOf(value = false)
        private set

    var firstDieState by mutableStateOf(value = DieState())
    var secondDieState by mutableStateOf(value = DieState())

    val isRolling: Boolean
        get() = isHolding || isUserRolling

    fun isDieRolling(diePosition: DiePosition): Boolean =
        isRolling && dieStateAt(diePosition = diePosition).isRollingLocally

    fun startHold(rollingPositions: Set<DiePosition>) {
        isArmed = false
        firstDieState =
            firstDieState.startingHold(isRolling = DiePosition.First in rollingPositions)
        secondDieState =
            secondDieState.startingHold(isRolling = DiePosition.Second in rollingPositions)
        isHolding = true
    }

    fun release(isDoubleTap: Boolean, commit: () -> Unit) {
        isHolding = false
        val shouldRoll = isArmed || isDoubleTap
        if (shouldRoll && !isUserRolling) {
            isUserRolling = true
            commit()
        }
    }

    fun applyRoll(diceRollState: DiceRollState) {
        firstDieState =
            firstDieState.applying(roll = diceRollState, diePosition = DiePosition.First)
        secondDieState =
            secondDieState.applying(roll = diceRollState, diePosition = DiePosition.Second)
    }

    internal fun registerShake() {
        isArmed = true
        shakeTick++
    }

    internal fun tick() {
        shakeTick++
    }

    internal fun setIsUserRollingToFalse() {
        isUserRolling = false
    }

    internal fun markDieSettled(diePosition: DiePosition, onDiceSettled: () -> Unit) {
        if (!dieStateAt(diePosition = diePosition).isSettling) return

        when (diePosition) {
            DiePosition.First -> firstDieState = firstDieState.copy(isSettling = false)
            DiePosition.Second -> secondDieState = secondDieState.copy(isSettling = false)
        }

        // TODO(SS-262): Need to make sure this works with Impersonate and whatever else. This kicks
        //  off the ArenaMatchCombatQueue's onCombatActionResolved()
        val isAnyDieStillSettling = firstDieState.isSettling || secondDieState.isSettling
        if (!isAnyDieStillSettling) {
            onDiceSettled()
        }
    }

    private fun dieStateAt(diePosition: DiePosition): DieState =
        when (diePosition) {
            DiePosition.First -> firstDieState
            DiePosition.Second -> secondDieState
        }

    private fun DieState.startingHold(isRolling: Boolean): DieState =
        if (isRolling) {
            copy(isRollingLocally = true, isEnabled = true, isSettling = true)
        } else {
            copy(isRollingLocally = false, isSettling = false)
        }

    private fun DieState.applying(roll: DiceRollState, diePosition: DiePosition): DieState =
        when (roll) {
            is DiceRollState.OneDie ->
                when {
                    diePosition == roll.diePosition ->
                        copy(
                            value = roll.value,
                            isEnabled = true,
                            tint = roll.dieColor.toColor(),
                            badge = roll.diceBadge.forPosition(diePosition = diePosition),
                            settleGeneration = settleGeneration + 1,
                            isRollingLocally = false,
                            isSettling = true,
                        )

                    roll.isSiblingDisabled ->
                        copy(
                            isEnabled = false,
                            tint = null,
                            badge = null,
                            isRollingLocally = false,
                            isSettling = false,
                        )

                    else -> this
                }

            is DiceRollState.TwoDice ->
                copy(
                    value = if (diePosition == DiePosition.First) roll.firstDie else roll.secondDie,
                    isEnabled = true,
                    tint = roll.dieColor.toColor(),
                    badge = roll.diceBadge.forPosition(diePosition = diePosition),
                    settleGeneration = settleGeneration + 1,
                    isRollingLocally = false,
                    isSettling = true,
                )
        }

    private fun DiceBadgeState?.forPosition(diePosition: DiePosition): DiceBadgeState? =
        this?.takeIf { it.diePosition == diePosition }
}
