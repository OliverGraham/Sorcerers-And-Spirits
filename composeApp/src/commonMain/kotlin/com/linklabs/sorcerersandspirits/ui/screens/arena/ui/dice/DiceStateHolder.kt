package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the two dice rest on ones, as a normal attack would show them.
private const val DIE_RESTING_FACE = 1
private const val BUZZ_LINGER_MILLIS = 200L
private const val ROLL_SHAKE_INTERVAL_MILLIS = 220L
private const val ROLL_SHAKE_COUNT = 7

data class DieState(
    val value: Int = DIE_RESTING_FACE,
    val isEnabled: Boolean = true,
    val tint: Color? = null,
    val badge: DiceBadgeState? = null,
    val settleGeneration: Int = 0,
    val isRollingLocally: Boolean = false,
    val isSettling: Boolean = false,
)

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

    private var hasReportedSettleForRoll = true

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

    fun startRolling(diceRollState: DiceRollState) {
        isUserRolling = true
        firstDieState =
            firstDieState.startingHold(isRolling = diceRollState.rolls(diePosition = DiePosition.First))
        secondDieState =
            secondDieState.startingHold(isRolling = diceRollState.rolls(diePosition = DiePosition.Second))
    }

    fun applyRoll(diceRollState: DiceRollState) {
        hasReportedSettleForRoll = false
        firstDieState = firstDieState.applying(roll = diceRollState, diePosition = DiePosition.First)
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
        when (diePosition) {
            DiePosition.First -> firstDieState = firstDieState.copy(isSettling = false)
            DiePosition.Second -> secondDieState = secondDieState.copy(isSettling = false)
        }

        // TODO: Need to make sure this works with Impersonate and whatever else. This kicks off the
        //       ArenaMatchCombatQueue's onCombatActionResolved()

        val isAnyDieStillSettling = firstDieState.isSettling || secondDieState.isSettling
        if (!isAnyDieStillSettling && !hasReportedSettleForRoll) {
            hasReportedSettleForRoll = true
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
                if (diePosition == roll.diePosition) {
                    copy(
                        value = roll.value,
                        isEnabled = true,
                        tint = roll.dieColor.toColor(),
                        badge = roll.diceBadge.forPosition(diePosition = diePosition),
                        settleGeneration = settleGeneration + 1,
                        isSettling = true,
                    )
                } else {
                    copy(
                        isEnabled = !roll.isSiblingDisabled,
                        tint = if (roll.isSiblingDisabled) null else roll.dieColor.toColor(),
                        badge = null,
                        isSettling = false,
                    )
                }

            is DiceRollState.TwoDice ->
                copy(
                    value = if (diePosition == DiePosition.First) roll.firstDie else roll.secondDie,
                    isEnabled = true,
                    tint = roll.dieColor.toColor(),
                    badge = roll.diceBadge.forPosition(diePosition = diePosition),
                    settleGeneration = settleGeneration + 1,
                    isSettling = true,
                )
        }

    private fun DiceBadgeState?.forPosition(diePosition: DiePosition): DiceBadgeState? =
        this?.takeIf { it.diePosition == diePosition }

    private fun DiceRollState.rolls(diePosition: DiePosition): Boolean =
        when (this) {
            is DiceRollState.OneDie -> this.diePosition == diePosition
            is DiceRollState.TwoDice -> true
        }
}

@Composable
internal fun rememberDiceStateHolder(diceRollState: DiceRollState?): DiceStateHolder {
    val diceStateHolder = remember { DiceStateHolder() }

    ShakeDiceWhileHeld(diceStateHolder = diceStateHolder)
    ShakeDiceWhileWaitingForResult(
        diceStateHolder = diceStateHolder,
        diceRollState = diceRollState,
    )

    return diceStateHolder
}

@Composable
private fun ShakeDiceWhileHeld(diceStateHolder: DiceStateHolder) {
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current

    LaunchedEffect(key1 = diceStateHolder.isHolding) {
        if (!diceStateHolder.isHolding) return@LaunchedEffect

        var stopBuzzJob: Job? = null
        try {
            shakeDetector.shakes.collect {
                diceStateHolder.registerShake()
                haptics.startContinuous(strength = HapticStrength.MEDIUM)
                stopBuzzJob?.cancel()
                stopBuzzJob = launch {
                    delay(timeMillis = BUZZ_LINGER_MILLIS)
                    haptics.stopContinuous()
                }
            }
        } finally {
            stopBuzzJob?.cancel()
            haptics.stopContinuous()
        }
    }
}

@Composable
private fun ShakeDiceWhileWaitingForResult(
    diceStateHolder: DiceStateHolder,
    diceRollState: DiceRollState?,
) {
    val haptics = LocalHaptics.current

    LaunchedEffect(key1 = diceRollState?.id) {
        if (diceRollState == null) return@LaunchedEffect

        diceStateHolder.startRolling(diceRollState = diceRollState)
        haptics.vibrate(strength = HapticStrength.HEAVY)

        repeat(times = ROLL_SHAKE_COUNT) {
            delay(timeMillis = ROLL_SHAKE_INTERVAL_MILLIS)
            diceStateHolder.tick()
        }

        diceStateHolder.applyRoll(diceRollState = diceRollState)
        diceStateHolder.setIsUserRollingToFalse()
    }
}
