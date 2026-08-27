package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val ANIMATED_SECONDS_LABEL = "AnimatedSeconds"
private const val ANIMATED_SECONDS_INITIAL_SCALE = 0.92f
private const val ANIMATED_SECONDS_SCALE_IN_DURATION_MILLIS = 220
private const val ANIMATED_SECONDS_SCALE_IN_DELAY_MILLIS = 90
private const val ANIMATED_SECONDS_SCALE_OUT_DURATION_MILLIS = 220

@Composable
internal fun AnimatedSeconds(
    seconds: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    AnimatedContent(
        targetState = seconds,
        modifier = modifier,
        transitionSpec = {
            scaleIn(
                initialScale = ANIMATED_SECONDS_INITIAL_SCALE,
                animationSpec =
                tween(
                    durationMillis = ANIMATED_SECONDS_SCALE_IN_DURATION_MILLIS,
                    delayMillis = ANIMATED_SECONDS_SCALE_IN_DELAY_MILLIS,
                ),
            ) togetherWith
                scaleOut(
                    animationSpec =
                    tween(durationMillis = ANIMATED_SECONDS_SCALE_OUT_DURATION_MILLIS),
                )
        },
        label = ANIMATED_SECONDS_LABEL,
    ) { animatedSeconds ->
        content(animatedSeconds)
    }
}
