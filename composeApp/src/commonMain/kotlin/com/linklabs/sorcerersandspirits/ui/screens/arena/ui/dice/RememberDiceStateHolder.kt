package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BUZZ_LINGER_MILLIS = 200L
private const val ROLL_TUMBLE_IMPULSE_MILLIS = 220L
private const val MAX_ROLL_TUMBLE_IMPULSES = 27

@Composable
internal fun rememberDiceStateHolder(
    diceRollState: DiceRollState?,
    userId: String,
): DiceStateHolder {
    val diceStateHolder = remember { DiceStateHolder() }

    ShakeDiceWhileHeld(diceStateHolder = diceStateHolder)
    ShakeDiceWhileWaitingForResult(diceStateHolder = diceStateHolder)
    ApplyRollWhenResolved(
        diceStateHolder = diceStateHolder,
        diceRollState = diceRollState,
        userId = userId,
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
private fun ShakeDiceWhileWaitingForResult(diceStateHolder: DiceStateHolder) {
    val haptics = LocalHaptics.current

    LaunchedEffect(key1 = diceStateHolder.isUserRolling) {
        if (!diceStateHolder.isUserRolling) return@LaunchedEffect

        haptics.vibrate(strength = HapticStrength.HEAVY)
        repeat(times = MAX_ROLL_TUMBLE_IMPULSES) {
            delay(timeMillis = ROLL_TUMBLE_IMPULSE_MILLIS)
            diceStateHolder.tick()
        }

        diceStateHolder.setIsUserRollingToFalse()
    }
}

@Composable
private fun ApplyRollWhenResolved(
    diceStateHolder: DiceStateHolder,
    diceRollState: DiceRollState?,
    userId: String,
) {
    val onDiceSettled = ArenaActionsProvider.current.onDiceSettled
    LaunchedEffect(key1 = diceRollState?.id) {
        if (diceRollState == null) return@LaunchedEffect

        if (diceRollState.doNotPerformDiceRollAnimation) {
            onDiceSettled(/* userId= */ userId)
            return@LaunchedEffect
        }

        diceStateHolder.applyRoll(diceRollState = diceRollState)
    }
}
