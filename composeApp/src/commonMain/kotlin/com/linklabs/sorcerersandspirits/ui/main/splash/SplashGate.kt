package com.linklabs.sorcerersandspirits.ui.main.splash

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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

private const val SPLASH_GATE_CROSSFADE_LABEL = "splashGateCrossfade"
private const val CROSSFADE_DURATION_MILLIS = 1000
private const val LINK_LABS_MIN_DURATION_MILLIS = 1400L
private const val GAME_MIN_DURATION_MILLIS = 1800L

private sealed interface SplashStage {
    data class Splash(val phase: SplashPhase) : SplashStage

    data object Content : SplashStage
}

@Composable
internal fun SplashGate(
    isAppReady: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var stage by remember {
        mutableStateOf<SplashStage>(value = SplashStage.Splash(phase = SplashPhase.LinkLabs))
    }
    val latestIsAppReady = rememberUpdatedState(newValue = isAppReady)

    LaunchedEffect(key1 = Unit) {
        delay(timeMillis = LINK_LABS_MIN_DURATION_MILLIS)
        stage = SplashStage.Splash(phase = SplashPhase.Game)
        delay(timeMillis = GAME_MIN_DURATION_MILLIS)
        snapshotFlow { latestIsAppReady.value }.first { isReady -> isReady }
        stage = SplashStage.Content
    }

    Crossfade(
        targetState = stage,
        animationSpec = tween(
            durationMillis = CROSSFADE_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = SPLASH_GATE_CROSSFADE_LABEL,
        modifier = modifier,
    ) { currentStage ->
        when (currentStage) {
            is SplashStage.Splash -> SplashScreen(phase = currentStage.phase)
            is SplashStage.Content -> content()
        }
    }
}
