package com.linklabs.sorcerersandspirits.ui.screens.arena.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Wraps an avatar's visual [content] and plays a short [AvatarGesture] whenever [gestureKey]
 * changes. Motion is done with plain Compose [Animatable]s (no animation library) so basic "the
 * avatar reacts to what it did / had done to it" works immediately. A specific avatar can later be
 * swapped for a Rive state machine without changing this call site.
 *
 * @param facingRight flips directional motion (lunge/recoil) so an avatar on the right of the
 *   screen still lunges toward its target.
 * @param gestureKey a value that changes per event (e.g. the triggering message id). The gesture
 *   replays each time this changes; passing a stable value keeps the avatar at rest.
 */
@Composable
internal fun CombatAvatar(
    gesture: AvatarGesture,
    gestureKey: Any?,
    modifier: Modifier = Modifier,
    facingRight: Boolean = true,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(initialValue = 0f) }
    val offsetY = remember { Animatable(initialValue = 0f) }
    val rotation = remember { Animatable(initialValue = 0f) }
    val scale = remember { Animatable(initialValue = 1f) }
    val alpha = remember { Animatable(initialValue = 1f) }

    val direction = if (facingRight) 1f else -1f

    LaunchedEffect(gestureKey, gesture) {
        // Reset to rest before replaying so repeated identical gestures still read as motion.
        offsetX.snapTo(targetValue = 0f)
        offsetY.snapTo(targetValue = 0f)
        rotation.snapTo(targetValue = 0f)
        scale.snapTo(targetValue = 1f)
        alpha.snapTo(targetValue = 1f)

        when (gesture) {
            AvatarGesture.None -> Unit

            AvatarGesture.Lunge -> {
                offsetX.animateTo(targetValue = 40f * direction, animationSpec = tween(120))
                offsetX.animateTo(targetValue = 0f, animationSpec = tween(220))
            }

            AvatarGesture.Recoil -> {
                offsetX.animateTo(targetValue = -24f * direction, animationSpec = tween(90))
                offsetX.animateTo(targetValue = 0f, animationSpec = tween(260))
            }

            AvatarGesture.Shake -> {
                repeat(times = 4) {
                    offsetX.animateTo(targetValue = 12f, animationSpec = tween(50))
                    offsetX.animateTo(targetValue = -12f, animationSpec = tween(50))
                }
                offsetX.animateTo(targetValue = 0f, animationSpec = tween(50))
            }

            AvatarGesture.Cast -> {
                scale.animateTo(targetValue = 1.12f, animationSpec = tween(160))
                scale.animateTo(targetValue = 1f, animationSpec = tween(240))
            }

            AvatarGesture.Cheer -> {
                offsetY.animateTo(targetValue = -28f, animationSpec = tween(150))
                offsetY.animateTo(targetValue = 0f, animationSpec = tween(200))
            }

            AvatarGesture.Collapse -> {
                rotation.animateTo(targetValue = 80f * direction, animationSpec = tween(400))
                alpha.animateTo(targetValue = 0.4f, animationSpec = tween(400))
            }
        }
    }

    Box(
        modifier =
            modifier
                .offset {
                    IntOffset(x = offsetX.value.roundToInt(), y = offsetY.value.roundToInt())
                }
                .rotate(degrees = rotation.value)
                .scale(scale = scale.value)
                .alpha(alpha = alpha.value)
    ) {
        content()
    }
}
