package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/** How many segments a swept slash trail is sampled into, shared by every bowed slash. */
private const val SLASH_TRAIL_SAMPLES = 12

/** Smooth 0-to-1 ramp between [edge0] and [edge1], easing a blade out of its ready pose. */
internal fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(minimumValue = 0f, maximumValue = 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Interpolate an angle along the shorter arc, so a blade rolls the near way from one pose to
 * another.
 */
internal fun lerpAngle(fromDegrees: Float, toDegrees: Float, fraction: Float): Float {
    val delta = ((toDegrees - fromDegrees + 540f) % 360f) - 180f
    return fromDegrees + delta * fraction
}

/** A point along a bowed slash arc: the straight span pushed out by a sine-peaked bow. */
internal fun slashPoint(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    fraction: Float,
): Offset {
    val base = lerp(start = start, stop = end, fraction = fraction)
    val bowAmount = sin(x = fraction.toDouble() * PI).toFloat() * bow
    return base + perp * bowAmount
}

/** The arc's local direction at [fraction], sampled either side so a blade rides tip-first. */
internal fun slashTangent(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    fraction: Float,
): Offset {
    val ahead =
        slashPoint(start, end, perp, bow, (fraction + 0.02f).coerceAtMost(maximumValue = 1f))
    val behind =
        slashPoint(start, end, perp, bow, (fraction - 0.02f).coerceAtLeast(minimumValue = 0f))
    return ahead - behind
}

/** The polyline the slash edge has traced so far along its bowed arc, up to [progress]. */
internal fun slashTrailPath(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    progress: Float,
): Path =
    Path().apply {
        val origin = slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = 0f)
        moveTo(x = origin.x, y = origin.y)
        for (sample in 1..SLASH_TRAIL_SAMPLES) {
            val fraction = progress * sample / SLASH_TRAIL_SAMPLES
            val point =
                slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = fraction)
            lineTo(x = point.x, y = point.y)
        }
    }
