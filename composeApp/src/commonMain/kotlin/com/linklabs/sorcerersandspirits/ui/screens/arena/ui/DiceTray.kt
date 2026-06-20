package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Before any roll lands the tray rests on a pair of ones, matching a normal attack's two dice.
private const val DIE_RESTING_FACE = 1
private const val DEFAULT_DICE_COUNT = 2

// How long the continuous buzz lingers after the last detected shake, bridging brief gaps in
// movement so the buzz stays smooth instead of stuttering.
private const val BUZZ_LINGER_MILLIS = 200L

// A double-tap has no hold duration, so the dice tumble for this long before the roll is committed,
// giving the same sense of a thrown roll as holding and shaking does.
private const val DOUBLE_TAP_ROLL_MILLIS = 1000L

/**
 * The dice the whole arena watches (SS-112), and the only way to roll them (SS-112 follow-up):
 * there is no separate roll button. The tray shows the faces of the most recent roll by ANY player
 * and re-runs the tumble whenever that roll changes, so the user keeps seeing the dice they rolled
 * and watches them tumble to a new value when someone else rolls.
 *
 * On the user's turn, when a roll is available, [onRoll] is non-null and the dice become the roll
 * control: grab and shake the phone to throw them around the tray, then release to roll, or
 * double-tap to roll after a short tumble. When [onRoll] is null the dice are display-only.
 * [needsTargetHint] is true when a roll is waiting only on the user picking a target.
 */
@Composable
internal fun DiceTray(
    onRoll: (() -> Unit)?,
    needsTargetHint: Boolean,
    modifier: Modifier = Modifier,
) {
    val latestRollDice = LocalLatestRollDice.current
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()

    val diceCount = if (latestRollDice.isEmpty()) DEFAULT_DICE_COUNT else latestRollDice.size
    val dieFaces =
        remember(latestRollDice) {
            List(size = diceCount) { index ->
                latestRollDice.getOrElse(index = index) { DIE_RESTING_FACE }
            }
        }

    var isHolding by remember { mutableStateOf(value = false) }
    var isArmed by remember { mutableStateOf(value = false) }
    var isDoubleTapRolling by remember { mutableStateOf(value = false) }
    // Ticks once per detected shake so the dice get a fresh impulse without restarting their
    // physics
    // loop; only meaningful while held, which is the only time it is collected.
    var shakeTick by remember { mutableStateOf(value = 0) }
    val isRolling = isHolding || isDoubleTapRolling
    val isRollable = onRoll != null
    val currentOnRoll by rememberUpdatedState(newValue = onRoll)

    // While the dice are held, each phone shake kicks them around the tray, arms the roll so the
    // release commits it, and drives a smooth haptic buzz (SS-112).
    LaunchedEffect(key1 = isHolding) {
        if (!isHolding) return@LaunchedEffect
        var stopBuzzJob: Job? = null
        try {
            shakeDetector.shakes.collect {
                shakeTick++
                isArmed = true
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

    val commitRoll = {
        haptics.vibrate(strength = HapticStrength.HEAVY)
        currentOnRoll?.invoke()
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .holdToRollGesture(
                    enabled = isRollable,
                    onHoldStart = {
                        isArmed = false
                        isHolding = true
                    },
                    onRelease = { isDoubleTap ->
                        isHolding = false
                        if (!isDoubleTapRolling) {
                            when {
                                isDoubleTap -> {
                                    isDoubleTapRolling = true
                                    scope.launch {
                                        delay(timeMillis = DOUBLE_TAP_ROLL_MILLIS)
                                        isDoubleTapRolling = false
                                        commitRoll()
                                    }
                                }

                                isArmed -> commitRoll()
                            }
                        }
                    },
                )
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally)
        ) {
            dieFaces.forEach { face ->
                Die3d(value = face, isRolling = isRolling, enabled = true, shakeTick = shakeTick)
            }
        }

        val caption =
            diceCaption(
                isRollable = isRollable,
                needsTargetHint = needsTargetHint,
                isHolding = isHolding,
                isArmed = isArmed,
                isDoubleTapRolling = isDoubleTapRolling,
            )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun diceCaption(
    isRollable: Boolean,
    needsTargetHint: Boolean,
    isHolding: Boolean,
    isArmed: Boolean,
    isDoubleTapRolling: Boolean,
): String? =
    when {
        isDoubleTapRolling -> "Rolling…"
        isHolding && isArmed -> "Release to roll"
        isHolding -> "Shake!"
        needsTargetHint -> "Tap an enemy to target"
        isRollable -> "Shake or double-tap the dice to roll"
        else -> null
    }

/**
 * Press-and-hold gesture for the dice. Consumes the touch so any surrounding scroll can't steal it:
 * [onHoldStart] fires on press, and [onRelease] fires on lift with whether the press was the second
 * tap of a double-tap. Inert unless [enabled].
 */
private fun Modifier.holdToRollGesture(
    enabled: Boolean,
    onHoldStart: () -> Unit,
    onRelease: (isDoubleTap: Boolean) -> Unit,
): Modifier =
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
        var previousUpTimeMillis = 0L

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val isSecondTapOfDoubleTap =
                down.uptimeMillis - previousUpTimeMillis < doubleTapTimeoutMillis
            onHoldStart()

            val up = awaitPointerUp(pointerId = down.id)

            previousUpTimeMillis = up?.uptimeMillis ?: 0L
            onRelease(isSecondTapOfDoubleTap)
        }
    }

/**
 * Suspends until the pointer with [pointerId] lifts, consuming its events so the gesture keeps
 * ownership of the touch for the whole hold. Returns the lifting change, or null if the pointer
 * disappeared without a cleanup.
 */
private suspend fun AwaitPointerEventScope.awaitPointerUp(
    pointerId: PointerId
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        change.consume()
        if (!change.pressed) return change
    }
}
