package com.linklabs.sorcerersandspirits.ui.common.loading

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.ClassGradient
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

private const val GLOW_VISIBILITY_LABEL = "swinging_logo_glow_visibility"
private const val GLOW_CYCLE_MILLIS = 24_000
private const val GLOW_FADE_MILLIS = 1200
private const val LIGHTNING_STRIKE_MILLIS = 1500
private const val BOLT_COUNT = 7
private const val BOLT_SEGMENTS = 6
private const val BOLT_JITTER_FRACTION = 0.11f

private val sharedAnimationMillisState = mutableLongStateOf(value = 0L)

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
    val millis = sharedAnimationMillis()
    val visibility by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = GLOW_FADE_MILLIS),
        label = GLOW_VISIBILITY_LABEL,
    )

    val cyclingGradient = cyclingClassGradient(gradients = gradients, millis = millis)
    val innerColor = cyclingGradient.innerColor
    val outerColor = cyclingGradient.outerColor

    val pulseWave = oscillation(millis = millis, periodMillis = style.glowPulseDurationMillis * 2)
    val pulse = style.glowMinAlpha + (style.glowMaxAlpha - style.glowMinAlpha) * pulseWave

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = visibility * pulse }
                .background(brush = Brush.radialGradient(colors = listOf(innerColor, outerColor))),
        )
        if (style.hasLightning) {
            val strike = (millis % LIGHTNING_STRIKE_MILLIS).toFloat() / LIGHTNING_STRIKE_MILLIS
            ElectricBolts(
                boltColor = innerColor,
                strike = strike,
                alpha = visibility,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun ClassColorLightningBurst(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    originFraction: Offset = Offset(x = 0.5f, y = 0.5f),
    intensity: Float = 1f,
) {
    val gradients = remember { classFamilyGradients() }
    val millis = sharedAnimationMillis()
    val boltColor = cyclingClassGradient(gradients = gradients, millis = millis).innerColor
    val strike = (millis % LIGHTNING_STRIKE_MILLIS).toFloat() / LIGHTNING_STRIKE_MILLIS
    ElectricBolts(
        boltColor = boltColor,
        strike = strike,
        alpha = alpha,
        modifier = modifier,
        originFraction = originFraction,
        intensity = intensity,
    )
}

@Composable
internal fun SwingingLogo(style: SwingingLogoStyle, modifier: Modifier = Modifier) {
    val millis = sharedAnimationMillis()
    val swingWave = oscillation(millis = millis, periodMillis = style.swingDurationMillis * 2)
    val rotation = -style.swingDegrees + (style.swingDegrees * 2f) * swingWave

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

@Composable
private fun sharedAnimationMillis(): Long {
    LaunchedEffect(key1 = Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMillis ->
                sharedAnimationMillisState.longValue = frameTimeMillis
            }
        }
    }
    return sharedAnimationMillisState.longValue
}

@Composable
private fun ElectricBolts(
    boltColor: Color,
    strike: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    originFraction: Offset = Offset(x = 0.5f, y = 0.5f),
    intensity: Float = 1f,
) {
    val bolts = remember { electricBolts() }
    val boltPaths = remember { List(size = BOLT_COUNT) { Path() } }

    Canvas(modifier = modifier) {
        val coreWidth = size.minDimension * 0.004f * intensity
        val glowWidth = size.minDimension * 0.018f * intensity
        val jitterAmplitude = size.minDimension * BOLT_JITTER_FRACTION
        val origin = Offset(x = originFraction.x * size.width, y = originFraction.y * size.height)
        val screenCorners = listOf(
            Offset(x = 0f, y = 0f),
            Offset(x = size.width, y = 0f),
            Offset(x = 0f, y = size.height),
            Offset(x = size.width, y = size.height),
        )
        val reachToEdge = screenCorners.maxOf { corner -> (corner - origin).getDistance() }

        bolts.forEachIndexed { index, bolt ->
            val local = (strike + bolt.phaseOffset).mod(other = 1f)
            val flash = boltFlashAlpha(local = local, litFraction = bolt.litFraction) * alpha
            if (flash <= 0f) return@forEachIndexed

            val direction = Offset(x = cos(x = bolt.angleRadians), y = sin(x = bolt.angleRadians))
            val perpendicular = Offset(x = -direction.y, y = direction.x)
            val reach = reachToEdge * bolt.lengthFraction
            val path = boltPaths[index]
            path.rewind()
            (0..BOLT_SEGMENTS).forEach { segment ->
                val progress = segment.toFloat() / BOLT_SEGMENTS
                val taper = sin(x = progress * PI.toFloat())
                val along = origin + direction * (reach * progress)
                val point = along + perpendicular * (bolt.segmentJitter[segment] * jitterAmplitude * taper)
                if (segment == 0) {
                    path.moveTo(x = point.x, y = point.y)
                } else {
                    path.lineTo(x = point.x, y = point.y)
                }
            }

            drawPath(
                path = path,
                color = boltColor.copy(alpha = (flash * 0.6f * intensity).coerceAtMost(maximumValue = 1f)),
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

private fun cyclingClassGradient(gradients: List<ClassGradient>, millis: Long): CyclingGradient {
    val colorCycle = (millis % GLOW_CYCLE_MILLIS).toFloat() / GLOW_CYCLE_MILLIS * gradients.size
    val segment = colorCycle.toInt() % gradients.size
    val fraction = colorCycle - colorCycle.toInt()
    val from = gradients[segment]
    val to = gradients[(segment + 1) % gradients.size]
    return CyclingGradient(
        innerColor = lerp(start = from.topStop, stop = to.topStop, fraction = fraction),
        outerColor = lerp(start = from.bottomStop, stop = to.bottomStop, fraction = fraction),
    )
}

private class CyclingGradient(val innerColor: Color, val outerColor: Color)

private fun oscillation(millis: Long, periodMillis: Int): Float {
    val progress = (millis % periodMillis).toFloat() / periodMillis
    return (1f - cos(x = progress * 2f * PI.toFloat())) / 2f
}

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

private fun boltFlashAlpha(local: Float, litFraction: Float): Float {
    val isLit = local < litFraction
    if (!isLit) return 0f
    val position = local / litFraction
    return 1f - abs(x = position * 2f - 1f)
}

private class ElectricBolt(
    val angleRadians: Float,
    val lengthFraction: Float,
    val segmentJitter: List<Float>,
    val phaseOffset: Float,
    val litFraction: Float,
    val thickness: Float,
)
