package com.linklabs.sorcerersandspirits.ui.common.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyGradients
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.sorcerers_and_spirits_small_logo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

sealed interface SwingingLogoStyle {
    val swingDurationMillis: Int
    val swingDegrees: Float
    val glowPulseDurationMillis: Int
    val glowMinAlpha: Float
    val glowMaxAlpha: Float
    val hasLightning: Boolean

    data object Energetic : SwingingLogoStyle {
        override val swingDurationMillis = 900
        override val swingDegrees = 11f
        override val glowPulseDurationMillis = 1400
        override val glowMinAlpha = 0.55f
        override val glowMaxAlpha = 1f
        override val hasLightning = true
    }

    data object Mellow : SwingingLogoStyle {
        override val swingDurationMillis = 1600
        override val swingDegrees = 8f
        override val glowPulseDurationMillis = 3200
        override val glowMinAlpha = 0.35f
        override val glowMaxAlpha = 0.7f
        override val hasLightning = false
    }
}

private const val LOGO_SWING_LABEL = "swinging_logo_swing"
private const val LOGO_SWING_ROTATION_LABEL = "swinging_logo_swing_rotation"
private const val GLOW_LABEL = "swinging_logo_glow"
private const val GLOW_COLOR_LABEL = "swinging_logo_glow_color"
private const val GLOW_PULSE_LABEL = "swinging_logo_glow_pulse"
private const val GLOW_VISIBILITY_LABEL = "swinging_logo_glow_visibility"
private const val LIGHTNING_LABEL = "swinging_logo_lightning"
private const val LIGHTNING_STRIKE_LABEL = "swinging_logo_lightning_strike"
private const val GLOW_CYCLE_MILLIS = 24_000
private const val GLOW_FADE_MILLIS = 1200
private const val LIGHTNING_STRIKE_MILLIS = 1500
private const val BOLT_COUNT = 7
private const val BOLT_SEGMENTS = 6
private const val BOLT_JITTER_FRACTION = 0.11f

@Composable
internal fun SwingingLogoLoader(
    modifier: Modifier = Modifier,
    style: SwingingLogoStyle = SwingingLogoStyle.Energetic,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ClassColorGlowBackground(style = style, isVisible = true)
        SwingingLogo(style = style)
    }
}

@Composable
internal fun ClassColorGlowBackground(
    style: SwingingLogoStyle,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val gradients = remember { classFamilyGradients() }
    val transition = rememberInfiniteTransition(label = GLOW_LABEL)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = gradients.size.toFloat(),
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = GLOW_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = GLOW_COLOR_LABEL,
    )
    val pulse by transition.animateFloat(
        initialValue = style.glowMinAlpha,
        targetValue = style.glowMaxAlpha,
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = style.glowPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = GLOW_PULSE_LABEL,
    )
    val visibility by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = GLOW_FADE_MILLIS),
        label = GLOW_VISIBILITY_LABEL,
    )

    val segment = phase.toInt() % gradients.size
    val fraction = phase - phase.toInt()
    val from = gradients[segment]
    val to = gradients[(segment + 1) % gradients.size]
    val innerColor = lerp(start = from.topStop, stop = to.topStop, fraction = fraction)
    val outerColor = lerp(start = from.bottomStop, stop = to.bottomStop, fraction = fraction)

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = visibility * pulse }
                .background(brush = Brush.radialGradient(colors = listOf(innerColor, outerColor))),
        )
        if (style.hasLightning) {
            ElectricBolts(boltColor = innerColor, alpha = visibility, modifier = Modifier.fillMaxSize())
        }
    }
}

private class ElectricBolt(
    val angleRadians: Float,
    val lengthFraction: Float,
    val segmentJitter: List<Float>,
    val phaseOffset: Float,
    val litFraction: Float,
    val thickness: Float,
)

private fun electricBolts(): List<ElectricBolt> {
    val random = Random(seed = 0x5C0E)
    return (0 until BOLT_COUNT).map { index ->
        val baseAngle = index.toFloat() / BOLT_COUNT * 2f * PI.toFloat()
        val angleJitter = (random.nextFloat() - 0.5f) * 0.6f
        val jitter = (0..BOLT_SEGMENTS).map { segment ->
            val isEndpoint = segment == 0 || segment == BOLT_SEGMENTS
            if (isEndpoint) 0f else random.nextFloat() * 2f - 1f
        }
        ElectricBolt(
            angleRadians = baseAngle + angleJitter,
            lengthFraction = 0.95f + random.nextFloat() * 0.45f,
            segmentJitter = jitter,
            phaseOffset = random.nextFloat(),
            litFraction = 0.1f + random.nextFloat() * 0.12f,
            thickness = 0.7f + random.nextFloat() * 0.6f,
        )
    }
}

@Composable
private fun ElectricBolts(boltColor: Color, alpha: Float, modifier: Modifier = Modifier) {
    val bolts = remember { electricBolts() }
    val transition = rememberInfiniteTransition(label = LIGHTNING_LABEL)
    val strike by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = LIGHTNING_STRIKE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = LIGHTNING_STRIKE_LABEL,
    )

    Canvas(modifier = modifier) {
        val coreWidth = size.minDimension * 0.004f
        val glowWidth = size.minDimension * 0.018f
        val jitterAmplitude = size.minDimension * BOLT_JITTER_FRACTION
        val maxRadius = max(a = size.width, b = size.height) / 2f

        bolts.forEach { bolt ->
            val local = (strike + bolt.phaseOffset).mod(other = 1f)
            val flash = boltFlashAlpha(local = local, litFraction = bolt.litFraction) * alpha
            if (flash <= 0f) return@forEach

            val direction = Offset(x = cos(x = bolt.angleRadians), y = sin(x = bolt.angleRadians))
            val perpendicular = Offset(x = -direction.y, y = direction.x)
            val reach = maxRadius * bolt.lengthFraction
            val path = Path()
            (0..BOLT_SEGMENTS).forEach { segment ->
                val progress = segment.toFloat() / BOLT_SEGMENTS
                val taper = sin(x = progress * PI.toFloat())
                val along = center + direction * (reach * progress)
                val point = along + perpendicular * (bolt.segmentJitter[segment] * jitterAmplitude * taper)
                if (segment == 0) {
                    path.moveTo(x = point.x, y = point.y)
                } else {
                    path.lineTo(x = point.x, y = point.y)
                }
            }

            drawPath(
                path = path,
                color = boltColor.copy(alpha = flash * 0.6f),
                style = Stroke(width = glowWidth * bolt.thickness, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = flash),
                style = Stroke(width = coreWidth * bolt.thickness, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun boltFlashAlpha(local: Float, litFraction: Float): Float {
    val isLit = local < litFraction
    if (!isLit) return 0f
    val position = local / litFraction
    return 1f - abs(x = position * 2f - 1f)
}

@Composable
internal fun SwingingLogo(style: SwingingLogoStyle, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = LOGO_SWING_LABEL)
    val rotation by transition.animateFloat(
        initialValue = -style.swingDegrees,
        targetValue = style.swingDegrees,
        animationSpec =
        infiniteRepeatable(
            animation =
            tween(durationMillis = style.swingDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = LOGO_SWING_ROTATION_LABEL,
    )

    Image(
        painter = painterResource(resource = Res.drawable.sorcerers_and_spirits_small_logo),
        contentDescription = "Sorcerers and Spirits logo",
        modifier =
        modifier
            .size(size = 200.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                rotationZ = rotation
            },
    )
}

@Preview
@Composable
private fun SwingingLogoLoaderEnergeticPreview() {
    Box(modifier = Modifier.fillMaxSize().background(color = Color.Black)) {
        SwingingLogoLoader(style = SwingingLogoStyle.Energetic)
    }
}

@Preview
@Composable
private fun SwingingLogoLoaderMellowPreview() {
    Box(modifier = Modifier.fillMaxSize().background(color = Color.Black)) {
        SwingingLogoLoader(style = SwingingLogoStyle.Mellow)
    }
}
