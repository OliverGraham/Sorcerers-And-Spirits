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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the two dice rest on ones, as a normal attack would show them.
private const val DIE_RESTING_FACE = 1
private const val BUZZ_LINGER_MILLIS = 200L
private const val ROLL_TUMBLE_IMPULSE_MILLIS = 220L
private const val MAX_ROLL_TUMBLE_IMPULSES = 27

data class DieState(
    val value: Int = DIE_RESTING_FACE,
    val isEnabled: Boolean = true,
    val tint: Color? = null,
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

    val isRolling: Boolean
        get() = isHolding || isUserRolling

    val isSettled: Boolean
        get() = !isRolling && !firstDieState.isSettling && !secondDieState.isSettling

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

    internal fun registerShake() {
        isArmed = true
        shakeTick++
    }

    internal fun tick() {
        shakeTick++
    }

    internal fun finishAwaiting() {
        isUserRolling = false
    }

    internal fun markDieSettled(diePosition: DiePosition) {
        when (diePosition) {
            DiePosition.First -> firstDieState = firstDieState.copy(isSettling = false)
            DiePosition.Second -> secondDieState = secondDieState.copy(isSettling = false)
        }
    }

    private fun dieStateAt(diePosition: DiePosition): DieState =
        when (diePosition) {
            DiePosition.First -> firstDieState
            DiePosition.Second -> secondDieState
        }

    // TODO: Likely not necessary - maybe call if diceRollState is null???

    /**
     * Stop awaiting a committed roll whose turn already resolved without a roll-bearing result:
     * newer messages have landed since the roll was released but none rolled the dice, so tumbling
     * on to the safety cap would only stall on a stale value. A whole turn's messages arrive in one
     * batch, so a batch that carries no roll is a turn that produced none for this device.
     */
    internal fun settleIfResolvedWithoutResult() {
        val hasTurnResolvedWithoutRoll = isUserRolling
        if (hasTurnResolvedWithoutRoll) finishAwaiting()
    }

    internal fun commitRoll(diceRollState: DiceRollState) {
        firstDieState =
            firstDieState.applying(diceRollState = diceRollState, at = DiePosition.First)
        secondDieState =
            secondDieState.applying(diceRollState = diceRollState, at = DiePosition.Second)
        isUserRolling = false
    }
}

private fun DieState.startingHold(isRolling: Boolean): DieState =
    if (isRolling) {
        copy(isRollingLocally = true, isEnabled = true)
    } else {
        copy(isRollingLocally = false)
    }

private fun DieState.applying(diceRollState: DiceRollState, at: DiePosition): DieState {
    val rolledValue = diceRollState.rolledValueAt(diePosition = at)

    return when {
        rolledValue != null ->
            copy(
                value = rolledValue,
                isEnabled = true,
                tint = diceRollState.tintAt(diePosition = at),
                settleGeneration = settleGeneration + 1,
                isRollingLocally = false,
                isSettling = true,
            )

        at == diceRollState.disabledDiePosition -> copy(isEnabled = false, isRollingLocally = false)

        else -> copy(isRollingLocally = false)
    }
}

@Composable
internal fun rememberDiceStateHolder(): DiceStateHolder {
    val diceStateHolder = remember { DiceStateHolder() }

    diceStateHolder.settleIfResolvedWithoutResult()

    ShakeDiceWhileHeld(diceStateHolder = diceStateHolder)
    ShakeDiceWhileWaitingForResult(diceStateHolder = diceStateHolder)

    return diceStateHolder
}

@Composable
private fun ShakeDiceWhileHeld(diceStateHolder: DiceStateHolder) {
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current

    // TODO: Not supposed to use mutableState for keys for LaunchedEffect
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
private fun ShakeDiceWhileWaitingForResult(diceStateHolder: DiceStateHolder) {
    val haptics = LocalHaptics.current

    // TODO: Not supposed to use mutableState for keys for LaunchedEffect
    LaunchedEffect(key1 = diceStateHolder.isUserRolling) {
        if (!diceStateHolder.isUserRolling) return@LaunchedEffect

        haptics.vibrate(strength = HapticStrength.HEAVY)
        repeat(times = MAX_ROLL_TUMBLE_IMPULSES) {
            delay(timeMillis = ROLL_TUMBLE_IMPULSE_MILLIS)
            diceStateHolder.tick()
        }

        diceStateHolder.finishAwaiting()
    }
}
