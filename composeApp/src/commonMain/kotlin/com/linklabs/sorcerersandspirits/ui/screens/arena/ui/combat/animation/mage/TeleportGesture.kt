package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.snapToRest
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.stretchTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TELEPORT_REACT_MILLIS = 120L
private const val TELEPORT_ENTER_MILLIS = 300
private const val TELEPORT_GONE_MILLIS = 220L
private const val TELEPORT_RETURN_MILLIS = 260
private const val TELEPORT_LUNGE_MILLIS = 160
private const val TELEPORT_RECOIL_MILLIS = 180
private const val TELEPORT_GRAY_MILLIS = 320
private const val TELEPORT_ENTER_SCALE = 0.5f
private const val TELEPORT_RETURN_SCALE = 0.72f
private const val TELEPORT_GRAY_SATURATION = 0.12f

internal fun ArenaMessageState.getTeleportMessageOrNull(userId: String) =
    (this as? ArenaMessageState.Teleport)?.takeIf {
        it.mageUserId ==
            userId
    }

internal suspend fun AvatarGestureState.playTeleport(
    message: ArenaMessageState.Teleport,
    density: Density,
) {
    if (message !is ArenaMessageState.Teleport.Result) return

    snapToRest()
    if (message.didDefenderEscape) {
        teleportSuccess(density = density)
    } else {
        teleportFailure(density = density)
    }
}

internal fun ArenaMessageState.Teleport.teleportAnimationMilliseconds(): Long = when (this) {
    is ArenaMessageState.Teleport.Result ->
        if (didDefenderEscape) {
            TELEPORT_REACT_MILLIS +
                TELEPORT_ENTER_MILLIS +
                TELEPORT_GONE_MILLIS +
                TELEPORT_RETURN_MILLIS
        } else {
            TELEPORT_REACT_MILLIS +
                TELEPORT_LUNGE_MILLIS +
                maxOf(TELEPORT_RECOIL_MILLIS.toLong(), TELEPORT_GRAY_MILLIS.toLong())
        }

    else -> 0L
}

private suspend fun AvatarGestureState.teleportSuccess(density: Density) =
    with(receiver = density) {
        delay(timeMillis = TELEPORT_REACT_MILLIS)
        coroutineScope {
            launch {
                offsetY.animateTo(
                    targetValue = -60.dp.toPx(),
                    animationSpec =
                    tween(
                        durationMillis = TELEPORT_ENTER_MILLIS,
                        easing = FastOutLinearInEasing,
                    ),
                )
            }
            launch {
                stretchTo(
                    targetX = TELEPORT_ENTER_SCALE,
                    targetY = TELEPORT_ENTER_SCALE,
                    durationMillis = TELEPORT_ENTER_MILLIS,
                )
            }
            launch {
                teleportVanish.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = TELEPORT_ENTER_MILLIS),
                )
            }
        }

        delay(timeMillis = TELEPORT_GONE_MILLIS)

        offsetY.snapTo(targetValue = 0f)
        scaleX.snapTo(targetValue = TELEPORT_RETURN_SCALE)
        scaleY.snapTo(targetValue = TELEPORT_RETURN_SCALE)
        coroutineScope {
            launch {
                teleportVanish.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = TELEPORT_RETURN_MILLIS),
                )
            }
            launch {
                stretchTo(targetX = 1f, targetY = 1f, durationMillis = TELEPORT_RETURN_MILLIS)
            }
        }
    }

private suspend fun AvatarGestureState.teleportFailure(density: Density) =
    with(receiver = density) {
        delay(timeMillis = TELEPORT_REACT_MILLIS)
        offsetY.animateTo(
            targetValue = -24.dp.toPx(),
            animationSpec =
            tween(durationMillis = TELEPORT_LUNGE_MILLIS, easing = FastOutSlowInEasing),
        )
        coroutineScope {
            launch {
                offsetY.animateTo(
                    targetValue = 8.dp.toPx(),
                    animationSpec = tween(durationMillis = TELEPORT_RECOIL_MILLIS / 2),
                )
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = TELEPORT_RECOIL_MILLIS / 2),
                )
            }
            launch {
                saturation.animateTo(
                    targetValue = TELEPORT_GRAY_SATURATION,
                    animationSpec = tween(durationMillis = TELEPORT_GRAY_MILLIS),
                )
            }
        }
    }
