package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.AnimatedSeconds

private const val TIMER_PULSE_TRANSITION_LABEL = "TurnTimerPulseTransition"
private const val TIMER_PULSE_SCALE_LABEL = "TurnTimerPulseScale"
private const val TIMER_PULSE_DURATION_MILLIS = 500
private const val TIMER_PULSE_MAX_SCALE = 1.3f
private val TIMER_FONT_SIZE = 28.sp
private val TIMER_CORNER_PADDING = 12.dp

/**
 * The combat turn clock, pinned in the top-end corner. It shows only in the final stretch of a turn
 * ([isVisible], the last ~15s) and pulses in the player's [error] color as it runs out ([isUrgent],
 * the last ~10s). Presentation only — the 45s turn timer itself ticks in the ViewModel (SS-155).
 */
@Composable
internal fun TurnTimer(
    secondsRemaining: Int,
    isVisible: Boolean,
    isUrgent: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val pulseScale = if (isUrgent) timerPulseScale() else 1f
    val color =
        if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    AnimatedSeconds(
        seconds = secondsRemaining,
        modifier = modifier.padding(all = TIMER_CORNER_PADDING),
    ) { seconds ->
        Text(
            text = "$seconds",
            color = color,
            fontSize = TIMER_FONT_SIZE,
            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.SemiBold,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
        )
    }
}

/** The looping 1f→[TIMER_PULSE_MAX_SCALE] heartbeat the timer rides while it is urgent. */
@Composable
private fun timerPulseScale(): Float {
    val pulseTransition = rememberInfiniteTransition(label = TIMER_PULSE_TRANSITION_LABEL)
    return pulseTransition
        .animateFloat(
            initialValue = 1f,
            targetValue = TIMER_PULSE_MAX_SCALE,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = TIMER_PULSE_DURATION_MILLIS),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = TIMER_PULSE_SCALE_LABEL,
        )
        .value
}
