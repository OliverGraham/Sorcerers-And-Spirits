package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

private const val HOLDING_DIE_SCALE = 1.15f
private const val RESTING_DIE_SCALE = 1f
private const val HOLDING_DIE_SCALE_LABEL = "HoldingDieScale"

// How long the continuous buzz lingers after the last detected shake, bridging brief gaps in
// movement so the buzz stays smooth instead of stuttering.
private const val BUZZ_LINGER_MILLIS = 200L

/**
 * Roll the dice three ways (SS-069):
 * - press-and-hold and shake for as little or as long as you like, then release;
 * - double-tap the dice; or
 * - press the Roll button.
 *
 * The hold gesture consumes its touch (`awaitEachGesture` + `consume`) so the surrounding scrolling
 * combat list can't steal the press and cancel the hold. The Roll button and double-tap roll
 * without shaking, keeping turns playable on emulators and devices without a usable accelerometer.
 * Visuals are intentionally a placeholder; SS-077 and SS-078 own the final dice rendering and
 * styling.
 */
@Composable
internal fun HoldToRollDice(
    label: String,
    diceCount: Int,
    enabled: Boolean,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeDetector = LocalShakeDetector.current
    val haptics = LocalHaptics.current
    val currentOnRoll by rememberUpdatedState(newValue = onRoll)
    var isHolding by remember { mutableStateOf(value = false) }
    var isArmed by remember { mutableStateOf(value = false) }

    ContinuousBuzzWhileShaking(
        isHolding = isHolding,
        shakeDetector = shakeDetector,
        haptics = haptics,
        onShake = { isArmed = true },
    )

    val roll = {
        haptics.vibrate(strength = HapticStrength.HEAVY)
        currentOnRoll()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .holdToRollGesture(
                        enabled = enabled,
                        onHoldStart = {
                            isArmed = false
                            isHolding = true
                        },
                        onRelease = { isDoubleTap ->
                            isHolding = false
                            val shouldRoll = isArmed || isDoubleTap
                            if (shouldRoll) roll()
                        },
                    ),
            shape = RoundedCornerShape(size = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                    repeat(times = diceCount) {
                        DiePlaceholder(isHolding = isHolding, enabled = enabled)
                    }
                }
                Text(
                    text =
                        captionFor(
                            label = label,
                            enabled = enabled,
                            isHolding = isHolding,
                            isArmed = isArmed,
                        ),
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

        RollButton(enabled = enabled, onRoll = roll)
    }
}

@Composable
private fun RollButton(enabled: Boolean, onRoll: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onRoll, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Text(text = "Roll")
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

@Composable
private fun DiePlaceholder(isHolding: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val scale by
        animateFloatAsState(
            targetValue = if (isHolding) HOLDING_DIE_SCALE else RESTING_DIE_SCALE,
            label = HOLDING_DIE_SCALE_LABEL,
        )

    Surface(
        modifier = modifier.size(size = 48.dp).scale(scale = scale),
        shape = RoundedCornerShape(size = 10.dp),
        color =
            if (enabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        tonalElevation = 2.dp,
    ) {}
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
        else -> "Hold & shake, double-tap, or press Roll · $label"
    }
