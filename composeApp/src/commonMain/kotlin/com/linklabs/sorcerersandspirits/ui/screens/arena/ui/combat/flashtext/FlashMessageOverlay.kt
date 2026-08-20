package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flashtext

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashAnchor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarBoundsStateHolder
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val flashFontSize = 46.sp
private const val FLASH_OUTLINE_FRACTION = 0.09f
private const val FLASH_SWING_DEGREES = 7f
private const val FLASH_SWING_MILLIS = 900
private const val PI_FLOAT = 3.1415927f
private const val CONFETTI_COUNT = 14
private const val CONFETTI_MILLIS = 750
private const val CONFETTI_MIN_DISTANCE = 26f
private const val CONFETTI_MAX_DISTANCE = 96f
private const val CONFETTI_MIN_RADIUS = 3f
private const val CONFETTI_MAX_RADIUS = 6f

private const val AVATAR_HEAD_FRACTION = 0.28f
private const val CENTER_BANNER_FRACTION = 0.2f
private const val FLASH_ENTER_SCALE = 0.3f
private const val FLASH_EXIT_SCALE = 1.5f
private const val FLASH_FADE_MILLIS = 300

@Composable
internal fun FlashMessageOverlay(
    animatingMessage: ArenaMessageState?,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    val onFlashComplete = ArenaActionsProvider.current.onFlashComplete
    var overlaySize by remember { mutableStateOf(value = IntSize.Zero) }

    var shownMessage by remember { mutableStateOf<ArenaMessageState?>(value = null) }
    val flashMessage = animatingMessage?.takeIf { it.isFlash() }
    if (flashMessage != null) shownMessage = flashMessage

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    avatarBoundsStateHolder.reportOverlayBounds(boundsInRoot = it.boundsInRoot())
                }
                .onSizeChanged { overlaySize = it }
    ) {
        val message = shownMessage ?: return@Box
        val point =
            message.flashAnchor.resolvePoint(
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                overlaySize = overlaySize,
            )

        val scale = remember(message.id) { Animatable(initialValue = FLASH_ENTER_SCALE) }
        val alpha = remember(message.id) { Animatable(initialValue = 0f) }
        val swingDegrees = remember(message.id) { Animatable(initialValue = 0f) }

        LaunchedEffect(message.id) {
            coroutineScope {
                launch { alpha.animateTo(targetValue = 1f) }
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                )
            }

            swingDegrees.animateTo(targetValue = 0f, animationSpec = swingSpec())

            coroutineScope {
                launch {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = FLASH_FADE_MILLIS),
                    )
                }
                scale.animateTo(
                    targetValue = FLASH_EXIT_SCALE,
                    animationSpec = tween(durationMillis = FLASH_FADE_MILLIS),
                )
            }

            onFlashComplete()
        }

        ConfettiBurst(triggerKey = message, origin = point, modifier = Modifier.fillMaxSize())

        FlashText(
            message = message,
            swingDegrees = swingDegrees.value,
            modifier =
                Modifier.centeredAt(point = { point }).graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

@Composable
private fun FlashText(
    message: ArenaMessageState,
    swingDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val baseStyle =
        TextStyle(
            fontSize = flashFontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
    val outlineWidthPx =
        with(LocalDensity.current) { flashFontSize.toPx() } * FLASH_OUTLINE_FRACTION

    val text = message.toStringResource()
    val fillColor = message.actingClassType?.toColor() ?: Color.White

    Box(modifier = modifier.graphicsLayer { rotationZ = swingDegrees }) {
        Text(
            text = text,
            style =
                baseStyle.copy(
                    color = Color.Black,
                    drawStyle = Stroke(width = outlineWidthPx),
                    shadow =
                        Shadow(
                            color = Color.Black,
                            offset = Offset(x = 0f, y = 4f),
                            blurRadius = 12f,
                        ),
                ),
        )
        Text(text = text, style = baseStyle.copy(color = fillColor))
    }
}

private fun swingSpec() = keyframes {
    durationMillis = FLASH_SWING_MILLIS
    0f at 0
    FLASH_SWING_DEGREES at 130
    FLASH_SWING_DEGREES at 320
    FLASH_SWING_DEGREES * 0.6f at 500
    FLASH_SWING_DEGREES * 0.35f at 680
    0f at FLASH_SWING_MILLIS
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

private fun FlashAnchor.resolvePoint(
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    overlaySize: IntSize,
): Offset {
    val center = Offset(x = overlaySize.width / 2f, y = overlaySize.height * CENTER_BANNER_FRACTION)
    if (this !is FlashAnchor.Avatar) return center

    val seat = avatarBoundsStateHolder.boundsFor(userId = userId) ?: return center
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
