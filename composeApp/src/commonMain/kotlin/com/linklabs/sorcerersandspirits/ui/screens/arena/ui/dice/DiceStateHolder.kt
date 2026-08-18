package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceBadgeState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Before any roll lands the two dice rest on ones, as a normal attack would show them.
private const val DIE_RESTING_FACE = 1
private const val BUZZ_LINGER_MILLIS = 200L
private const val ROLL_SHAKE_INTERVAL_MILLIS = 220L
private const val MINIMUM_ROLL_DURATION_MILLIS = 1_000L

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
        if (!firstDieState.isSettling && !secondDieState.isSettling) {
            onDiceSettled()
        }
    }

    private fun dieStateAt(diePosition: DiePosition): DieState =
        when (diePosition) {
            DiePosition.First -> firstDieState
            DiePosition.Second -> secondDieState
        }
}

private fun DieState.startingHold(isRolling: Boolean): DieState =
    if (isRolling) {
        copy(isRollingLocally = true, isEnabled = true, isSettling = true)
    } else {
        copy(isRollingLocally = false, isSettling = false)
    }

@Composable
internal fun rememberDiceStateHolder(diceRollStateId: Int?): DiceStateHolder {
    val diceStateHolder = remember { DiceStateHolder() }

    ShakeDiceWhileHeld(diceStateHolder = diceStateHolder)
    ShakeDiceWhileWaitingForResult(
        diceStateHolder = diceStateHolder,
        diceRollStateId = diceRollStateId,
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
    diceRollStateId: Int?,
) {
    val haptics = LocalHaptics.current
    val diceRollStateId by rememberUpdatedState(newValue = diceRollStateId)

    LaunchedEffect(key1 = diceStateHolder.isUserRolling) {
        if (!diceStateHolder.isUserRolling) return@LaunchedEffect

        val diceRollStateIdWhenRollStarted = diceRollStateId
        haptics.vibrate(strength = HapticStrength.HEAVY)

        val shakeJob = launch {
            while (true) {
                delay(timeMillis = ROLL_SHAKE_INTERVAL_MILLIS)
                diceStateHolder.tick()
            }
        }

        delay(timeMillis = MINIMUM_ROLL_DURATION_MILLIS)
        snapshotFlow { diceRollStateId }.first { it != diceRollStateIdWhenRollStarted }

        shakeJob.cancel()
        diceStateHolder.setIsUserRollingToFalse()
    }
}
