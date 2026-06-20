package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.haptics.HapticStrength
import com.linklabs.sorcerersandspirits.domain.haptics.Haptics
import com.linklabs.sorcerersandspirits.domain.sensor.ShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// How long the continuous buzz lingers after the last detected shake, bridging brief gaps in
// movement so the buzz stays smooth instead of stuttering.
private const val BUZZ_LINGER_MILLIS = 200L

/**
 * A roll control that triggers an attack or ability two ways (SS-069):
 * - press-and-hold and shake for as little or as long as you like, then release; or
 * - double-tap it.
 *
 * The hold gesture consumes its touch (`awaitEachGesture` + `consume`) so the surrounding scrolling
 * combat list can't steal the press and cancel the hold. Double-tap rolls without shaking, keeping
 * turns playable on emulators and devices without a usable accelerometer. The dice themselves now
 * live in the persistent [DiceTray] (SS-112); [onHoldChange] reports the hold so the tray's dice
 * spin while the control is held and settle on the result when released.
 */
@Composable
internal fun HoldToRollDice(
    label: String,
    enabled: Boolean,
    onRoll: () -> Unit,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current
    val currentOnRoll by rememberUpdatedState(newValue = onRoll)
    val currentOnHoldChange by rememberUpdatedState(newValue = onHoldChange)
    var isHolding by remember { mutableStateOf(value = false) }
    var isArmed by remember { mutableStateOf(value = false) }

    ContinuousBuzzWhileShaking(
        isHolding = isHolding,
        shakeDetector = shakeDetector,
        haptics = haptics,
        onShake = { isArmed = true },
    )

    // If this control leaves composition mid-hold (a state update ends the turn before the finger
    // lifts), the release never fires, so settle the shared tray here rather than leave it
    // spinning.
    DisposableEffect(key1 = Unit) { onDispose { currentOnHoldChange(false) } }

    val roll = {
        haptics.vibrate(strength = HapticStrength.HEAVY)
        currentOnRoll()
    }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .holdToRollGesture(
                    enabled = enabled,
                    onHoldStart = {
                        isArmed = false
                        isHolding = true
                        currentOnHoldChange(true)
                    },
                    onRelease = { isDoubleTap ->
                        isHolding = false
                        currentOnHoldChange(false)
                        val shouldRoll = isArmed || isDoubleTap
                        if (shouldRoll) roll()
                    },
                ),
        shape = RoundedCornerShape(size = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Text(
            text =
                captionFor(
                    label = label,
                    enabled = enabled,
                    isHolding = isHolding,
                    isArmed = isArmed,
                ),
            modifier = Modifier.padding(all = 16.dp).fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
        )
    }
}

/**
 * Runs one continuous, smooth buzz while the dice are held and being shaken. Each detected shake
 * reports via [onShake] and refreshes a linger window, so brief gaps in movement don't stutter the
 * buzz; it stops once movement ceases or the hold ends.
 */
@Composable
private fun ContinuousBuzzWhileShaking(
    isHolding: Boolean,
    shakeDetector: ShakeDetector,
    haptics: Haptics,
    onShake: () -> Unit,
) {
    LaunchedEffect(key1 = isHolding) {
        if (!isHolding) return@LaunchedEffect
        var stopBuzzJob: Job? = null
        try {
            shakeDetector.shakes.collect {
                onShake()
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

/**
 * Press-and-hold gesture for the dice. Consumes the touch so the surrounding scrolling list can't
 * steal it: [onHoldStart] fires on press, and [onRelease] fires on lift with whether the press was
 * the second tap of a double-tap.
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
 * ownership of the touch (and the parent scroll never takes over) for the whole hold. Returns the
 * lifting change, or `null` if the pointer disappeared without a cleanup.
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

private fun captionFor(
    label: String,
    enabled: Boolean,
    isHolding: Boolean,
    isArmed: Boolean,
): String =
    when {
        !enabled -> label
        isHolding && isArmed -> "Release to roll"
        isHolding -> "Shake!"
        else -> "Hold & shake or double-tap · $label"
    }
