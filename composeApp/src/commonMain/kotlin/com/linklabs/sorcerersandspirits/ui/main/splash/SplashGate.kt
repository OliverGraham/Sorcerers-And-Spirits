package com.linklabs.sorcerersandspirits.ui.main.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val PHASE_CROSSFADE_LABEL = "splashGatePhaseCrossfade"
private const val CROSSFADE_DURATION_MILLIS = 1000
private const val LINK_LABS_MIN_DURATION_MILLIS = 1400L
private const val GAME_MIN_DURATION_MILLIS = 1800L

@Composable
internal fun SplashGate(
    canRevealContent: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (isContentRevealed: Boolean) -> Unit,
) {
    var isSplashVisible by remember { mutableStateOf(value = true) }
    var isContentRevealed by remember { mutableStateOf(value = false) }
    var phase by remember { mutableStateOf(value = SplashPhase.LinkLabs) }
    val latestCanRevealContent = rememberUpdatedState(newValue = canRevealContent)

    LaunchedEffect(key1 = Unit) {
        delay(timeMillis = LINK_LABS_MIN_DURATION_MILLIS)
        phase = SplashPhase.Game
        delay(timeMillis = GAME_MIN_DURATION_MILLIS)
        snapshotFlow { latestCanRevealContent.value }.first { canReveal -> canReveal }
        isSplashVisible = false
        delay(timeMillis = CROSSFADE_DURATION_MILLIS.toLong())
        isContentRevealed = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        content(isContentRevealed)
        AnimatedVisibility(
            visible = isSplashVisible,
            enter = EnterTransition.None,
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = CROSSFADE_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            ),
        ) {
            Crossfade(
                targetState = phase,
                animationSpec = tween(
                    durationMillis = CROSSFADE_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
                label = PHASE_CROSSFADE_LABEL,
            ) { currentPhase ->
                SplashScreen(phase = currentPhase, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
