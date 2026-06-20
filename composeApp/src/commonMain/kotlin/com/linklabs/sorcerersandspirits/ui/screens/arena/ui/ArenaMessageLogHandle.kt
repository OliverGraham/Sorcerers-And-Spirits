package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.AnimatedSeconds
import com.linklabs.sorcerersandspirits.ui.common.WeightSpacer
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.delay

private const val TIMER_PULSE_TRANSITION_LABEL = "TimerPulseTransition"
private const val TIMER_PULSE_SCALE_LABEL = "TimerPulseScale"
private const val TIMER_PULSE_DURATION_MILLIS = 500
private const val TIMER_PULSE_MAX_SCALE = 1.3f

@Composable
internal fun ArenaMessageLogHandle(
    secondsRemaining: Int,
    isTimerVisible: Boolean,
    isTimerUrgent: Boolean,
    latestMessage: ArenaMessageState?,
    unhandledMessagesCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isTimerVisible) {
            AnimatedSeconds(seconds = secondsRemaining) { seconds ->
                TimerText(seconds = seconds, isUrgent = isTimerUrgent)
            }
        }

        WeightSpacer()

        // TODO(SS-025) Arena: Improve animation
        AnimatedContent(
            targetState = latestMessage,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 1000, delayMillis = 90),
                ) togetherWith scaleOut(animationSpec = tween(durationMillis = 1000))
            },
        ) { displayedMessage ->
            if (displayedMessage != null) {
                Text(text = displayedMessage.getDisplayString())
            }

            if (unhandledMessagesCount > 0) {
                val onIncomingMessageHandled = ArenaActionsProvider.current.onIncomingMessageHandled
                LaunchedEffect(key1 = transition.currentState, key2 = transition.targetState) {
                    if (transition.currentState == transition.targetState) {
                        delay(timeMillis = 1000)
                        onIncomingMessageHandled()
                    }
                }
            }
        }
        WeightSpacer()
    }
}

@Composable
private fun TimerText(seconds: Int, isUrgent: Boolean) {
    val scale =
        if (isUrgent) {
            val pulseTransition = rememberInfiniteTransition(label = TIMER_PULSE_TRANSITION_LABEL)
            pulseTransition
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
        } else {
            1f
        }

    Text(
        text = "$seconds",
        color =
            if (isUrgent) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal,
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogHandlePreview() {
    SorcerersAndSpiritsTheme {
        ArenaMessageLogHandle(
            secondsRemaining = 9,
            isTimerVisible = true,
            isTimerUrgent = true,
            latestMessage = ArenaMessageState.NewRound(id = "1", roundNumber = "1"),
            unhandledMessagesCount = 0,
        )
    }
}
