package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.finished

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarImage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.stretchTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val CHEER_HOP_UP_MILLIS = 240
private const val CHEER_HOP_DOWN_MILLIS = 200
private const val CHEER_LAND_SQUASH_MILLIS = 120
private const val CHEER_HOP_HEIGHT_DP = 20f
private const val CHEER_TILT_DEGREES = 7f

sealed interface VictoryDance {
    data object Cheer : VictoryDance
}

internal fun victoryDanceFor(classType: CharacterClassType): VictoryDance = when (classType) {
    else -> VictoryDance.Cheer
}

@Composable
internal fun VictoryDanceAvatar(classType: CharacterClassType, modifier: Modifier = Modifier) {
    val gestureState = remember { AvatarGestureState() }
    val density = LocalDensity.current
    val dance = victoryDanceFor(classType = classType)

    LaunchedEffect(key1 = dance) {
        while (true) {
            gestureState.playVictoryDance(dance = dance, density = density)
        }
    }

    AvatarImage(
        classType = classType,
        modifier = modifier.graphicsLayer { applyGesture(gestureState = gestureState) },
    )
}

internal suspend fun AvatarGestureState.playVictoryDance(dance: VictoryDance, density: Density) {
    when (dance) {
        is VictoryDance.Cheer -> cheer(density = density)
    }
}

private suspend fun AvatarGestureState.cheer(density: Density) {
    val hopPx = with(receiver = density) { CHEER_HOP_HEIGHT_DP.dp.toPx() }
    cheerHop(hopPx = hopPx, tiltDegrees = CHEER_TILT_DEGREES)
    cheerHop(hopPx = hopPx, tiltDegrees = -CHEER_TILT_DEGREES)
}

private suspend fun AvatarGestureState.cheerHop(hopPx: Float, tiltDegrees: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -hopPx,
                animationSpec = tween(durationMillis = CHEER_HOP_UP_MILLIS, easing = FastOutSlowInEasing),
            )
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = CHEER_HOP_DOWN_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = tiltDegrees,
                animationSpec = tween(durationMillis = CHEER_HOP_UP_MILLIS),
            )
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = CHEER_HOP_DOWN_MILLIS),
            )
        }
        launch {
            stretchTo(targetX = 0.96f, targetY = 1.06f, durationMillis = CHEER_HOP_UP_MILLIS)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = CHEER_HOP_DOWN_MILLIS)
        }
    }
    stretchTo(targetX = 1.08f, targetY = 0.92f, durationMillis = CHEER_LAND_SQUASH_MILLIS)
    stretchTo(targetX = 1f, targetY = 1f, durationMillis = CHEER_LAND_SQUASH_MILLIS)
}
