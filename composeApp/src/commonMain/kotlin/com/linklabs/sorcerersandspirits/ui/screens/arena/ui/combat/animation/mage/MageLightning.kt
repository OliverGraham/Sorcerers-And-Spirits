package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** How many kinks the arc breaks into, and how many storm clouds and strike flashes churn above. */
private const val BOLT_SEGMENTS = 6
private const val STORM_CLOUD_COUNT = 7
private const val STORM_STRIKE_FREQUENCY = 5f

/**
 * The Lightningbolt (SS-198): a single jagged arc of electricity crackling from [tail] to
 * [position], reseeded by its own moving head so it flickers alive as it flies. A [Thunderstorm]
 * amplifies the same arc into a storm-lit barrage (SS-210).
 */
internal fun DrawScope.drawLightningBolt(tail: Offset, position: Offset, alpha: Float) {
    val palette = MageSpellElement.Lightning.palette()
    val seed = position.x * 0.7f + position.y * 1.3f
    val arc = jaggedPath(from = tail, to = position, amplitudePx = 9.dp.toPx(), seed = seed)

    drawPath(
        path = arc,
        color = palette.glow,
        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.4f,
    )
    drawPath(
        path = arc,
        color = palette.core,
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha,
    )

    val forkStart = lerp(start = tail, stop = position, fraction = 0.55f)
    val forkTip =
        forkStart + Offset(x = (mageNoise(seed = seed) - 0.5f), y = -0.4f) * (22.dp.toPx())
    val fork =
        jaggedPath(from = forkStart, to = forkTip, amplitudePx = 5.dp.toPx(), seed = seed + 9f)
    drawPath(
        path = fork,
        color = palette.core,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.8f,
    )

    val haloRadius = 15.dp.toPx()
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(palette.core, palette.glow.copy(alpha = 0.5f), Color.Transparent),
                center = position,
                radius = haloRadius,
            ),
        radius = haloRadius,
        center = position,
        alpha = alpha * 0.85f,
    )
}

/**
 * The electric burst where a bolt lands (SS-198): a white flash with jagged sparks cracking
 * outward, sized by [scale] as it swells and cleared by [alpha] as it fades.
 */
internal fun DrawScope.drawLightningBurst(center: Offset, scale: Float, alpha: Float) {
    if (scale <= 0f) return
    val palette = MageSpellElement.Lightning.palette()
    val radius = 42.dp.toPx() * scale

    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(palette.core, palette.glow.copy(alpha = 0.7f), Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = alpha,
    )

    val sparkCount = 7
    for (index in 0 until sparkCount) {
        val radians = (index * 360f / sparkCount) * PI.toFloat() / 180f
        val direction = Offset(x = cos(x = radians), y = sin(x = radians))
        val spark =
            jaggedPath(
                from = center + direction * (radius * 0.2f),
                to = center + direction * (radius * 1.15f),
                amplitudePx = 6.dp.toPx(),
                seed = center.x + index * 17f,
            )
        drawPath(
            path = spark,
            color = palette.core,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = alpha * 0.85f,
        )
    }
}

/**
 * The Thunderstorm (SS-210): the whole battlefield darkens under a low bank of storm clouds, and
 * lightning strikes crack down toward [epicenter] — the opponent, never the Mage — in white flashes
 * paced by [swirl]. Faded in and out by [alpha].
 */
internal fun DrawScope.drawThunderstorm(epicenter: Offset, swirl: Float, alpha: Float) {
    if (alpha <= 0f) return
    val palette = MageSpellElement.Lightning.palette()

    drawRect(color = palette.atmosphere, alpha = alpha * 0.55f)
    drawStormClouds(swirl = swirl, palette = palette, alpha = alpha)

    val phase = swirl * STORM_STRIKE_FREQUENCY
    val tick = floor(x = phase)
    val withinTick = phase - tick
    val isStriking = mageNoise(seed = tick) > 0.5f
    val flash = if (isStriking) (1f - withinTick) * (1f - withinTick) else 0f
    if (flash <= 0.02f) return

    drawRect(color = palette.core, alpha = alpha * flash * 0.4f)
    val strikeOriginX = size.width * mageNoise(seed = tick + 3.1f)
    val strikeBolt =
        jaggedPath(
            from = Offset(x = strikeOriginX, y = 0f),
            to = epicenter,
            amplitudePx = 26.dp.toPx(),
            seed = tick,
        )
    drawPath(
        path = strikeBolt,
        color = palette.glow,
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * flash * 0.6f,
    )
    drawPath(
        path = strikeBolt,
        color = palette.core,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * flash,
    )
}

/** A low, drifting bank of dark storm clouds hanging over the top of the battlefield. */
private fun DrawScope.drawStormClouds(swirl: Float, palette: MageElementPalette, alpha: Float) {
    val cloudColor = palette.atmosphere.copy(alpha = 0.9f)
    for (index in 0 until STORM_CLOUD_COUNT) {
        val drift = cos(x = swirl * 2f * PI.toFloat() * 0.15f + index) * size.width * 0.05f
        val centerX = size.width * ((index + 0.5f) / STORM_CLOUD_COUNT) + drift
        val centerY = size.height * (0.06f + mageNoise(seed = index.toFloat()) * 0.12f)
        val radius = size.width * (0.16f + mageNoise(seed = index + 7f) * 0.08f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(cloudColor, Color.Transparent),
                    center = Offset(x = centerX, y = centerY),
                    radius = radius,
                ),
            radius = radius,
            center = Offset(x = centerX, y = centerY),
            alpha = alpha,
        )
    }
}

/**
 * A jagged, lightning-style polyline from [from] to [to], its kinks pushed sideways by
 * [amplitudePx] and tapered to nothing at both ends. [seed] varies the break pattern
 * deterministically.
 */
internal fun DrawScope.jaggedPath(from: Offset, to: Offset, amplitudePx: Float, seed: Float): Path {
    val direction = to - from
    val length = direction.getDistance()
    val path = Path().apply { moveTo(x = from.x, y = from.y) }
    if (length == 0f) return path

    val unit = direction / length
    val perpendicular = Offset(x = -unit.y, y = unit.x)
    for (index in 1 until BOLT_SEGMENTS) {
        val fraction = index.toFloat() / BOLT_SEGMENTS
        val base = lerp(start = from, stop = to, fraction = fraction)
        val jitter =
            (mageNoise(seed = seed + index * 13.1f) * 2f - 1f) *
                amplitudePx *
                sin(x = fraction * PI.toFloat())
        val point = base + perpendicular * jitter
        path.lineTo(x = point.x, y = point.y)
    }
    path.lineTo(x = to.x, y = to.y)
    return path
}
