package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How many fire pillars stand around the arena and how many embers rise through the Inferno. */
private const val INFERNO_PILLAR_COUNT = 6
private const val INFERNO_EMBER_COUNT = 24

/**
 * The Fireball (SS-198): a flaming ball hurled from [tail] to [position] on a trail of embers, its
 * core flickering alive as it flies. An [Inferno] amplifies the same ball into a barrage under
 * walls of fire (SS-210).
 */
internal fun DrawScope.drawFireball(tail: Offset, position: Offset, alpha: Float) {
    val palette = MageSpellElement.Fire.palette()

    drawLine(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.trail.copy(alpha = 0.8f), palette.glow),
                start = tail,
                end = position,
            ),
        start = tail,
        end = position,
        strokeWidth = 9.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.8f,
    )

    val emberCount = 4
    for (index in 1..emberCount) {
        val fraction = index.toFloat() / (emberCount + 1)
        val emberSeed = position.x + index * 11f
        val center =
            lerp(start = position, stop = tail, fraction = fraction) +
                Offset(
                    x = (mageNoise(seed = emberSeed) - 0.5f),
                    y = (mageNoise(seed = emberSeed + 3f) - 0.5f),
                ) * 10.dp.toPx()
        drawCircle(
            color = palette.glow,
            radius = (3f - fraction * 2f).dp.toPx(),
            center = center,
            alpha = alpha * (1f - fraction) * 0.8f,
        )
    }

    val ballRadius = 16.dp.toPx()
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.core,
                        palette.glow,
                        palette.trail.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                center = position,
                radius = ballRadius,
            ),
        radius = ballRadius,
        center = position,
        alpha = alpha,
    )
}

/**
 * The explosion where a Fireball lands (SS-198): a roiling burst of flame with tongues licking
 * outward, sized by [scale] as it swells and cleared by [alpha] as it fades.
 */
internal fun DrawScope.drawFireBurst(center: Offset, scale: Float, alpha: Float) {
    if (scale <= 0f) return
    val palette = MageSpellElement.Fire.palette()
    val radius = 46.dp.toPx() * scale

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.core,
                        palette.glow,
                        palette.trail.copy(alpha = 0.7f),
                        Color.Transparent,
                    ),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = alpha,
    )

    val tongueCount = 9
    for (index in 0 until tongueCount) {
        val radians = (index * 360f / tongueCount) * PI.toFloat() / 180f
        val direction = Offset(x = cos(x = radians), y = sin(x = radians))
        val reach = radius * (0.9f + mageNoise(seed = center.x + index * 13f) * 0.5f)
        drawCircle(
            color = palette.glow,
            radius = radius * 0.22f,
            center = center + direction * reach,
            alpha = alpha * 0.7f,
        )
    }
}

/**
 * The Inferno (SS-210): the whole battlefield glows red and pillars of fire stand up around the
 * arena, licking and flickering with [swirl], brightest around [epicenter] — the opponent, never
 * the Mage — while embers rise through it. Faded in and out by [alpha].
 */
internal fun DrawScope.drawInferno(epicenter: Offset, swirl: Float, alpha: Float) {
    if (alpha <= 0f) return
    val palette = MageSpellElement.Fire.palette()
    val span = maxOf(size.width, size.height)

    drawRect(color = palette.atmosphere, alpha = alpha * 0.22f)
    val glowRadius = span * 0.9f
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.glow.copy(alpha = 0.45f),
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

    for (index in 0 until INFERNO_PILLAR_COUNT) {
        drawFirePillar(index = index, swirl = swirl, palette = palette, alpha = alpha)
    }
    drawInfernoEmbers(swirl = swirl, palette = palette, alpha = alpha)
}

/** One wavering pillar of fire rising from the arena floor, its height flickering with [swirl]. */
private fun DrawScope.drawFirePillar(
    index: Int,
    swirl: Float,
    palette: MageElementPalette,
    alpha: Float,
) {
    val centerX = size.width * ((index + 0.5f) / INFERNO_PILLAR_COUNT)
    val flicker = 0.5f + 0.5f * sin(x = swirl * 2f * PI.toFloat() * (1.5f + index * 0.2f) + index)
    val height = size.height * (0.42f + 0.32f * flicker)
    val baseY = size.height
    val halfWidth = size.width * 0.06f
    val tipWobble = (mageNoise(seed = index + swirl) - 0.5f) * halfWidth

    val flame =
        Path().apply {
            moveTo(x = centerX - halfWidth, y = baseY)
            lineTo(x = centerX - halfWidth * 0.4f, y = baseY - height * 0.5f)
            lineTo(x = centerX + tipWobble, y = baseY - height)
            lineTo(x = centerX + halfWidth * 0.4f, y = baseY - height * 0.5f)
            lineTo(x = centerX + halfWidth, y = baseY)
            close()
        }
    drawPath(
        path = flame,
        brush =
            Brush.verticalGradient(
                colors = listOf(palette.core, palette.glow, palette.trail),
                startY = baseY - height,
                endY = baseY,
            ),
        alpha = alpha * 0.85f,
    )
}

/** Sparks lifting off the pillars, rising and drifting through the field with the [swirl]. */
private fun DrawScope.drawInfernoEmbers(swirl: Float, palette: MageElementPalette, alpha: Float) {
    for (index in 0 until INFERNO_EMBER_COUNT) {
        val rise = (swirl * (0.6f + (index % 5) * 0.12f) + mageNoise(seed = index.toFloat())) % 1f
        val x =
            size.width * mageNoise(seed = index + 5f) +
                sin(x = rise * PI.toFloat() * 2f) * 12.dp.toPx()
        val y = size.height * (1f - rise)
        drawCircle(
            color = palette.core,
            radius = (1.5f + (index % 3)).dp.toPx(),
            center = Offset(x = x, y = y),
            alpha = alpha * (1f - rise) * 0.7f,
        )
    }
}
