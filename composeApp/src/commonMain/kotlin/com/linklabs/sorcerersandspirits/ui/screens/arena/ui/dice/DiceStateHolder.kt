package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

data class DieState(val value: Int = DIE_RESTING_FACE, val isActive: Boolean = true)

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

    var settlingDice by mutableStateOf(value = emptySet<DiePosition>())
        private set

    var rollGeneration by mutableStateOf(value = 0)
        private set

    var firstDieState by mutableStateOf(value = DieState())
    var secondDieState by mutableStateOf(value = DieState())

    val isRolling: Boolean
        get() = isHolding || isUserRolling

    val isSettled: Boolean
        get() = !isRolling && settlingDice.isEmpty()

    // TODO: Refactor rollingDiePositions
    fun startHold(rollingPositions: Set<DiePosition>) {
        isArmed = false
        firstDieState = firstDieState.copy(isActive = DiePosition.First in rollingPositions)
        secondDieState = secondDieState.copy(isActive = DiePosition.Second in rollingPositions)
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
        settlingDice = settlingDice - diePosition
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
        settlingDice =
            when (diceRollState) {
                is DiceRollState.OneDie -> diceRollState.commitRoll()
                is DiceRollState.TwoDice -> diceRollState.commitRoll()
            }

        isUserRolling = false
        rollGeneration++
    }

    private fun DiceRollState.OneDie.commitRoll(): Set<DiePosition> {
        when (diePosition) {
            DiePosition.First -> {
                firstDieState = DieState(value = value, isActive = true)
                secondDieState = secondDieState.copy(isActive = false)
            }

            DiePosition.Second -> {
                firstDieState = firstDieState.copy(isActive = false)
                secondDieState = DieState(value = value, isActive = true)
            }
        }

        return setOf(diePosition)
    }

    private fun DiceRollState.TwoDice.commitRoll(): Set<DiePosition> {
        firstDieState = DieState(value = firstDie, isActive = true)
        secondDieState = DieState(value = secondDie, isActive = true)

        return setOf(DiePosition.First, DiePosition.Second)
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
