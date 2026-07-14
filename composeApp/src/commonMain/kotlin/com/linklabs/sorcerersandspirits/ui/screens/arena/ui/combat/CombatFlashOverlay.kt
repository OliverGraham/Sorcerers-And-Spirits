package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashAnchor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CombatAvatarBounds
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val flashFontSize = 26.sp
private const val PI_FLOAT = 3.1415927f
private const val CONFETTI_COUNT = 14
private const val CONFETTI_MILLIS = 750
private const val CONFETTI_MIN_DISTANCE = 26f
private const val CONFETTI_MAX_DISTANCE = 96f
private const val CONFETTI_MIN_RADIUS = 3f
private const val CONFETTI_MAX_RADIUS = 6f

private const val AVATAR_HEAD_FRACTION = 0.28f
private const val CENTER_BANNER_FRACTION = 0.2f

@Composable
internal fun FlashOverlay(
    playback: FlashPlaybackState,
    avatarBounds: CombatAvatarBounds,
    modifier: Modifier = Modifier,
) {
    var overlaySize by remember { mutableStateOf(value = IntSize.Zero) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    avatarBounds.reportFullScreenOverlayBounds(boundsInRoot = it.boundsInRoot())
                }
                .onSizeChanged { overlaySize = it }
    ) {
        val message = playback.currentMessage ?: return@Box
        val callout = playback.currentCallout ?: return@Box
        val point =
            playback.currentAnchor.resolvePoint(
                avatarBounds = avatarBounds,
                overlaySize = overlaySize,
            )

        if (playback.visibleState.targetState || playback.visibleState.currentState) {
            ConfettiBurst(
                triggerKey = message.id,
                origin = point,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visibleState = playback.visibleState,
            enter =
                scaleIn(
                    initialScale = 0.3f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
            modifier = Modifier.centeredAt(point = { point }),
        ) {
            FlashText(callout = callout)
        }
    }
}

private fun FlashAnchor.resolvePoint(
    avatarBounds: CombatAvatarBounds,
    overlaySize: IntSize,
): Offset {
    val center = Offset(x = overlaySize.width / 2f, y = overlaySize.height * CENTER_BANNER_FRACTION)
    if (this !is FlashAnchor.Avatar) return center

    val seat = avatarBounds.fullScreenBoundsFor(userId = userId) ?: return center
    return Offset(x = seat.center.x, y = seat.top + seat.height * AVATAR_HEAD_FRACTION)
}

private fun Modifier.centeredAt(point: () -> Offset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints = constraints)
    layout(width = placeable.width, height = placeable.height) {
        val target = point()
        placeable.place(
            x = (target.x - placeable.width / 2f).roundToInt(),
            y = (target.y - placeable.height / 2f).roundToInt(),
        )
    }
}

@Composable
private fun FlashText(callout: FlashCallout, modifier: Modifier = Modifier) {
    val color = callout.actingClassType?.toColor() ?: Color.White
    Text(
        text = callout.text(),
        style =
            TextStyle(
                fontSize = flashFontSize,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center,
                shadow =
                    Shadow(color = Color.Black, offset = Offset(x = 0f, y = 3f), blurRadius = 6f),
            ),
        modifier = modifier,
    )
}

@Composable
private fun ConfettiBurst(triggerKey: Any, origin: Offset, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(initialValue = 0f) }
    val particles = remember(key1 = triggerKey) { confettiParticles() }

    LaunchedEffect(key1 = triggerKey) {
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = CONFETTI_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        val fade = (1f - t).coerceIn(minimumValue = 0f, maximumValue = 1f)
        particles.forEach { particle ->
            val travelPx = particle.distance.dp.toPx() * t
            val position =
                Offset(
                    x = origin.x + cos(x = particle.angle) * travelPx,
                    y = origin.y + sin(x = particle.angle) * travelPx,
                )
            drawCircle(
                color = particle.color.copy(alpha = fade),
                radius = particle.radius.dp.toPx() * (1f - 0.5f * t),
                center = position,
            )
        }
    }
}

private class ConfettiParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color,
)

private fun confettiParticles(): List<ConfettiParticle> {
    val colors = classFamilyColors()

    return List(size = CONFETTI_COUNT) { index ->
        ConfettiParticle(
            angle = Random.nextFloat() * 2f * PI_FLOAT,
            distance =
                CONFETTI_MIN_DISTANCE +
                    Random.nextFloat() * (CONFETTI_MAX_DISTANCE - CONFETTI_MIN_DISTANCE),
            radius =
                CONFETTI_MIN_RADIUS +
                    Random.nextFloat() * (CONFETTI_MAX_RADIUS - CONFETTI_MIN_RADIUS),
            color = colors[index % colors.size],
        )
    }
}
