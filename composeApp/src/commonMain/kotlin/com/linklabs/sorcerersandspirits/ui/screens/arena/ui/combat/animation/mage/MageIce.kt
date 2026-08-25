package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The angle a snowflake's six arms are spaced by, and the golden angle the dome scatters flakes on.
 */
private const val SNOWFLAKE_ARM_DEGREES = 60f
private const val FLAKE_SCATTER_RADIANS = 2.399963f

/**
 * How many drifting flakes churn in the Blizzard, the gust streaks driving them, and the swirl's
 * vertical squash. Tuned dense and wide so the storm engulfs the whole battlefield (SS-210).
 */
private const val BLIZZARD_FLAKE_COUNT = 96
private const val BLIZZARD_GUST_COUNT = 7
private const val BLIZZARD_SWIRL_FLATTEN = 0.82f

/**
 * The Icebolt (SS-198): a jagged shard of ice on a trail of frozen wind and snowflakes, pointed
 * along its flight from [tail] to [position]. A [Blizzard] amplifies the same shard into a barrage.
 */
internal fun DrawScope.drawIcebolt(tail: Offset, position: Offset, alpha: Float) {
    val palette = MageSpellElement.Ice.palette()
    drawIceTrail(tail = tail, position = position, palette = palette, alpha = alpha)

    val flight = position - tail
    val degrees = atan2(y = flight.y, x = flight.x) * 180f / PI.toFloat() - 90f
    rotate(degrees = degrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawIceShard(palette = palette, alpha = alpha)
        }
    }
}

/**
 * The stream of frozen wind trailing the shard, brightening to its core, dusted with snowflakes.
 */
private fun DrawScope.drawIceTrail(
    tail: Offset,
    position: Offset,
    palette: MageElementPalette,
    alpha: Float,
) {
    drawLine(
        brush =
        Brush.linearGradient(
            colors = listOf(Color.Transparent, palette.trail.copy(alpha = 0.7f), palette.core),
            start = tail,
            end = position,
        ),
        start = tail,
        end = position,
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.75f,
    )

    val flakeCount = 4
    for (index in 1..flakeCount) {
        val fraction = index.toFloat() / (flakeCount + 1)
        val center = lerp(start = position, stop = tail, fraction = fraction)
        drawSnowflake(
            center = center,
            radius = (5f - fraction * 3f).dp.toPx(),
            color = palette.trail,
            alpha = alpha * (1f - fraction) * 0.8f,
        )
    }
}

/** The bolt's head: a soft frost halo behind a crystalline shard filled from core to glow. */
private fun DrawScope.drawIceShard(palette: MageElementPalette, alpha: Float) {
    val length = 30.dp.toPx()
    val halfWidth = 9.dp.toPx()

    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(palette.core, palette.glow.copy(alpha = 0.5f), Color.Transparent),
            center = Offset.Zero,
            radius = length,
        ),
        radius = length,
        center = Offset.Zero,
        alpha = alpha * 0.7f,
    )

    val shard =
        Path().apply {
            moveTo(x = 0f, y = -length)
            lineTo(x = halfWidth, y = 0f)
            lineTo(x = 0f, y = length * 0.4f)
            lineTo(x = -halfWidth, y = 0f)
            close()
        }
    drawPath(
        path = shard,
        brush =
        Brush.linearGradient(
            colors = listOf(palette.core, palette.glow),
            start = Offset(x = 0f, y = -length),
            end = Offset(x = 0f, y = length * 0.4f),
        ),
        alpha = alpha,
    )
}

/**
 * The shatter where an Icebolt lands (SS-198): a cold flash of frost with a ring of ice slivers
 * spraying outward, sized by [scale] as the burst swells and cleared by [alpha] as it fades.
 */
internal fun DrawScope.drawIceShatter(center: Offset, scale: Float, alpha: Float) {
    if (scale <= 0f) return
    val palette = MageSpellElement.Ice.palette()
    val radius = 40.dp.toPx() * scale

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

    val sliverCount = 8
    for (index in 0 until sliverCount) {
        val radians = (index * 360f / sliverCount) * PI.toFloat() / 180f
        val direction = Offset(x = cos(x = radians), y = sin(x = radians))
        drawLine(
            color = palette.core,
            start = center + direction * (radius * 0.35f),
            end = center + direction * (radius * 1.05f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * 0.85f,
        )
    }
}

/**
 * The Blizzard (SS-210): a driving storm of icy wind that engulfs the whole battlefield, centered
 * on [epicenter] — the opponent, never the Mage. A cold wash floods the field and brightens around
 * the epicenter, gusts streak across it, and [swirl] churns a wide, dense orbit of driven snow over
 * everything. Faded in and out by [alpha].
 */
internal fun DrawScope.drawBlizzardDome(epicenter: Offset, swirl: Float, alpha: Float) {
    if (alpha <= 0f) return
    val palette = MageSpellElement.Ice.palette()
    val span = maxOf(size.width, size.height)

    drawRect(color = palette.atmosphere, alpha = alpha * 0.24f)
    val glowRadius = span * 0.95f
    drawCircle(
        brush =
        Brush.radialGradient(
            colors =
            listOf(
                palette.atmosphere.copy(alpha = 0.5f),
                palette.atmosphere.copy(alpha = 0.2f),
                Color.Transparent,
            ),
            center = epicenter,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = epicenter,
        alpha = alpha,
    )

    drawBlizzardGusts(swirl = swirl, palette = palette, alpha = alpha)

    val maxOrbit = span * 0.85f
    for (index in 0 until BLIZZARD_FLAKE_COUNT) {
        val orbit = (0.08f + (index % 13) / 13f * 0.92f) * maxOrbit
        val spin = swirl * (0.4f + (index % 9) / 12f) * 2f * PI.toFloat()
        val angle = index * FLAKE_SCATTER_RADIANS + spin
        val center =
            epicenter +
                Offset(
                    x = cos(x = angle) * orbit,
                    y = sin(x = angle) * orbit * BLIZZARD_SWIRL_FLATTEN,
                )
        val flakeRadius = (2f + (index % 4)).dp.toPx()
        drawSnowflake(
            center = center,
            radius = flakeRadius,
            color = palette.core,
            alpha = alpha * (0.4f + (index % 5) / 5f * 0.55f),
        )
    }
}

/** Long streaks of wind-driven snow raked across the field, drifting with the [swirl]. */
private fun DrawScope.drawBlizzardGusts(swirl: Float, palette: MageElementPalette, alpha: Float) {
    val streakLength = size.width * 0.7f
    for (index in 0 until BLIZZARD_GUST_COUNT) {
        val drift = (swirl * (0.5f + (index % 3) * 0.2f)) % 1f
        val centerY = size.height * ((index + 0.5f) / BLIZZARD_GUST_COUNT)
        val centerX = (size.width * 1.3f) * (1f - drift) - size.width * 0.15f
        val start = Offset(x = centerX - streakLength / 2f, y = centerY)
        val end = Offset(x = centerX + streakLength / 2f, y = centerY - size.height * 0.05f)
        drawLine(
            brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.trail, Color.Transparent),
                start = start,
                end = end,
            ),
            start = start,
            end = end,
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * 0.35f,
        )
    }
}

/** A six-armed snowflake: three crossed slivers, shared by the bolt trail and the dome's drift. */
private fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    for (arm in 0 until 3) {
        val radians = (arm * SNOWFLAKE_ARM_DEGREES) * PI.toFloat() / 180f
        val reach = Offset(x = cos(x = radians) * radius, y = sin(x = radians) * radius)
        drawLine(
            color = color,
            start = center - reach,
            end = center + reach,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
    }
}
