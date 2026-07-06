package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp as lerpOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The ash the whole instrument drains toward as a failed Persuade lets the attack press on. */
private val persuadeAsh = Color(color = 0xFF8C8A85)

/** The pale sheen wood and the darker heartwood the flute's bore shades to. */
private val bardFluteLight = Color(color = 0xFFC79A5C)
private val bardFluteDark = Color(color = 0xFF6E4A24)

/** How far the lute leans off upright, neck up and to the right, as the Bard cradles it. */
private const val LUTE_TILT_DEGREES = -16f

/** The pear body's half-extents, the neck's reach and half-width, and the soundhole's radius. */
private val luteBodyHalfWidth = 33.dp
private val luteBodyHalfHeight = 42.dp
private val luteNeckLength = 74.dp
private val luteNeckHalfWidth = 6.dp
private val luteRosetteRadius = 11.dp

private const val LUTE_STRING_COUNT = 5
private const val STRING_VIBRATION_CYCLES = 3.5f
private val stringVibrationAmplitude = 4.dp

/** Lerp any colour toward the failure ash, so a whole scene greys out in step. */
private fun Color.grayed(fraction: Float): Color =
    lerp(start = this, stop = persuadeAsh, fraction = fraction)

/**
 * The Bard's lute (SS-207): a wood-bowled pear body with a bard-coloured rosette, a fretted neck
 * angling up to a pegbox, and strings drawn taut from the bridge to the pegs. [scale] pops the
 * whole instrument in; [alpha] fades it; [strum] rings the strings in a decaying standing wave;
 * [grayness] drains every colour toward ash as a failed Persuade lets the attack through.
 */
internal fun DrawScope.drawLute(
    anchor: Offset,
    accentColor: Color,
    alpha: Float,
    scale: Float,
    strum: Float,
    grayness: Float,
) {
    if (alpha <= 0f) return
    val wood = bardShellLight.grayed(fraction = grayness)
    val woodDark = bardShellDark.grayed(fraction = grayness)
    val accent = accentColor.grayed(fraction = grayness)

    rotate(degrees = LUTE_TILT_DEGREES, pivot = anchor) {
        scale(scale = scale, pivot = anchor) {
            drawLuteNeck(anchor = anchor, accent = accent, alpha = alpha)
            drawLuteBody(anchor = anchor, wood = wood, woodDark = woodDark, alpha = alpha)
            drawLuteRosette(anchor = anchor, accent = accent, woodDark = woodDark, alpha = alpha)
            drawLuteBridge(anchor = anchor, woodDark = woodDark, alpha = alpha)
            drawLuteStrings(anchor = anchor, strum = strum, alpha = alpha, grayness = grayness)
        }
    }
}

/** The neck joint at the top of the body, and the pegbox at the far end of the neck. */
private fun DrawScope.luteNeckBase(anchor: Offset): Offset =
    anchor - Offset(x = 0f, y = luteBodyHalfHeight.toPx())

private fun DrawScope.lutePegbox(anchor: Offset): Offset =
    luteNeckBase(anchor = anchor) - Offset(x = 0f, y = luteNeckLength.toPx())

/**
 * The fretted neck: a tapered wooden bar running up to the pegbox, banded by pale frets and pegs.
 */
private fun DrawScope.drawLuteNeck(anchor: Offset, accent: Color, alpha: Float) {
    val base = luteNeckBase(anchor = anchor)
    val pegbox = lutePegbox(anchor = anchor)
    val baseHalf = luteNeckHalfWidth.toPx()
    val pegHalf = baseHalf * 1.4f

    val neck =
        Path().apply {
            moveTo(x = base.x - baseHalf, y = base.y)
            lineTo(x = pegbox.x - pegHalf, y = pegbox.y)
            lineTo(x = pegbox.x + pegHalf, y = pegbox.y)
            lineTo(x = base.x + baseHalf, y = base.y)
            close()
        }
    drawPath(
        path = neck,
        brush =
            Brush.horizontalGradient(
                colors = listOf(bardStickDark, bardStickLight, bardStickDark),
                startX = base.x - baseHalf,
                endX = base.x + baseHalf,
            ),
        alpha = alpha,
    )

    val fretCount = 4
    for (fret in 1..fretCount) {
        val fraction = fret / (fretCount + 1f)
        val center = lerpOffset(start = base, stop = pegbox, fraction = fraction)
        val half = baseHalf + (pegHalf - baseHalf) * fraction
        drawLine(
            color = lerp(start = accent, stop = Color.White, fraction = 0.35f),
            start = Offset(x = center.x - half, y = center.y),
            end = Offset(x = center.x + half, y = center.y),
            strokeWidth = 1.4.dp.toPx(),
            alpha = alpha * 0.75f,
        )
    }

    val pegOffsets = listOf(-1f, 1f)
    pegOffsets.forEach { side ->
        drawCircle(
            color = bardStickLight,
            radius = 3.2.dp.toPx(),
            center = pegbox + Offset(x = side * pegHalf * 1.2f, y = 4.dp.toPx()),
            alpha = alpha,
        )
    }
}

/** How far a control point sits off the axis to bow the body's cubic sides into a rounded pear. */
private const val PEAR_SIDE_CONTROL = 1.08f

/** The wooden pear body, domed by a radial gradient so the bowl reads round rather than flat. */
private fun DrawScope.drawLuteBody(anchor: Offset, wood: Color, woodDark: Color, alpha: Float) {
    val halfWidth = luteBodyHalfWidth.toPx()
    val halfHeight = luteBodyHalfHeight.toPx()
    val top = anchor - Offset(x = 0f, y = halfHeight)

    val body =
        Path().apply {
            moveTo(x = top.x, y = top.y)
            cubicTo(
                x1 = anchor.x + halfWidth * PEAR_SIDE_CONTROL,
                y1 = anchor.y - halfHeight * 0.5f,
                x2 = anchor.x + halfWidth * 1.1f,
                y2 = anchor.y + halfHeight * 0.25f,
                x3 = anchor.x + halfWidth * 0.6f,
                y3 = anchor.y + halfHeight * 0.78f,
            )
            cubicTo(
                x1 = anchor.x + halfWidth * 0.28f,
                y1 = anchor.y + halfHeight * 1.04f,
                x2 = anchor.x - halfWidth * 0.28f,
                y2 = anchor.y + halfHeight * 1.04f,
                x3 = anchor.x - halfWidth * 0.6f,
                y3 = anchor.y + halfHeight * 0.78f,
            )
            cubicTo(
                x1 = anchor.x - halfWidth * 1.1f,
                y1 = anchor.y + halfHeight * 0.25f,
                x2 = anchor.x - halfWidth * PEAR_SIDE_CONTROL,
                y2 = anchor.y - halfHeight * 0.5f,
                x3 = top.x,
                y3 = top.y,
            )
            close()
        }
    drawPath(
        path = body,
        brush =
            Brush.radialGradient(
                colors = listOf(wood, woodDark),
                center = Offset(x = anchor.x - halfWidth * 0.35f, y = anchor.y - halfHeight * 0.2f),
                radius = halfHeight * 1.25f,
            ),
        alpha = alpha,
    )
    drawPath(
        path = body,
        brush =
            Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                startY = top.y,
                endY = anchor.y,
            ),
        alpha = alpha,
    )
}

/** The bard-coloured rosette ringing the soundhole, with the dark bore sunk at its centre. */
private fun DrawScope.drawLuteRosette(
    anchor: Offset,
    accent: Color,
    woodDark: Color,
    alpha: Float,
) {
    val center = anchor - Offset(x = 0f, y = luteBodyHalfHeight.toPx() * 0.12f)
    val radius = luteRosetteRadius.toPx()
    drawCircle(color = accent, radius = radius, center = center, alpha = alpha * 0.9f)
    drawCircle(
        color = lerp(start = accent, stop = Color.White, fraction = 0.4f),
        radius = radius * 0.78f,
        center = center,
        alpha = alpha * 0.9f,
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.Black, woodDark),
                center = center,
                radius = radius * 0.62f,
            ),
        radius = radius * 0.6f,
        center = center,
        alpha = alpha,
    )
}

/** The dark bridge below the soundhole where the strings anchor to the soundboard. */
private fun DrawScope.drawLuteBridge(anchor: Offset, woodDark: Color, alpha: Float) {
    val center = anchor + Offset(x = 0f, y = luteBodyHalfHeight.toPx() * 0.48f)
    drawRoundRectBar(
        center = center,
        halfWidth = 14.dp.toPx(),
        halfHeight = 3.dp.toPx(),
        color = woodDark,
        alpha = alpha,
    )
}

private fun DrawScope.drawRoundRectBar(
    center: Offset,
    halfWidth: Float,
    halfHeight: Float,
    color: Color,
    alpha: Float,
) {
    drawOval(
        color = color,
        topLeft = Offset(x = center.x - halfWidth, y = center.y - halfHeight),
        size = Size(width = halfWidth * 2f, height = halfHeight * 2f),
        alpha = alpha,
    )
}

/** The bridge and pegbox anchor points the strings run between, spread across the instrument. */
private fun DrawScope.stringBridgeAnchor(anchor: Offset): Offset =
    anchor + Offset(x = 0f, y = luteBodyHalfHeight.toPx() * 0.48f)

/**
 * The strings drawn taut from bridge to pegbox and, while [strum] rings, bowed side to side in a
 * decaying standing wave — each string pinned at both ends and belling out at its middle, the swell
 * fading as the ring dies away.
 */
private fun DrawScope.drawLuteStrings(anchor: Offset, strum: Float, alpha: Float, grayness: Float) {
    val bridge = stringBridgeAnchor(anchor = anchor)
    val pegbox = lutePegbox(anchor = anchor)
    val bridgeSpread = 11.dp.toPx()
    val pegSpread = 7.dp.toPx()
    val stringColor =
        lerp(start = Color.White, stop = bardStickLight, fraction = 0.3f)
            .grayed(fraction = grayness)

    val decay = 1f - strum
    val swell = sin(x = strum * PI.toFloat() * STRING_VIBRATION_CYCLES) * decay

    for (index in 0 until LUTE_STRING_COUNT) {
        val lateral = if (LUTE_STRING_COUNT == 1) 0f else index / (LUTE_STRING_COUNT - 1f) * 2f - 1f
        val start = bridge + Offset(x = lateral * bridgeSpread, y = 0f)
        val end = pegbox + Offset(x = lateral * pegSpread, y = 0f)
        val mid = lerpOffset(start = start, stop = end, fraction = 0.5f)
        val bow = swell * stringVibrationAmplitude.toPx() * (1f - abs(x = lateral) * 0.3f)
        val control = mid + Offset(x = bow, y = 0f)

        val string =
            Path().apply {
                moveTo(x = start.x, y = start.y)
                quadraticTo(x1 = control.x, y1 = control.y, x2 = end.x, y2 = end.y)
            }
        drawPath(
            path = string,
            color = stringColor,
            style = Stroke(width = 1.1.dp.toPx()),
            alpha = alpha * 0.85f,
        )
    }
}

/** How far the flute leans, its bore length and radius, and the spacing of its finger holes. */
private const val FLUTE_TILT_DEGREES = -22f
private val fluteLength = 152.dp
private val fluteRadius = 7.dp

/**
 * The charmer's flute the lute crossfades into (SS-207): a slim wooden bore leaning up to the
 * mouth, a warm sheen down its top, an embouchure near the head and finger holes along its length.
 * While [airFlow] runs, breathy wisps stream out of the far end, the instrument reading as
 * sounding.
 */
internal fun DrawScope.drawFlute(anchor: Offset, accentColor: Color, alpha: Float, airFlow: Float) {
    if (alpha <= 0f) return
    rotate(degrees = FLUTE_TILT_DEGREES, pivot = anchor) {
        val half = fluteLength.toPx() / 2f
        val head = anchor - Offset(x = half, y = 0f)
        val foot = anchor + Offset(x = half, y = 0f)
        val radius = fluteRadius.toPx()

        drawLine(
            brush =
                Brush.verticalGradient(
                    colors = listOf(bardFluteLight, bardFluteDark),
                    startY = anchor.y - radius,
                    endY = anchor.y + radius,
                ),
            start = head,
            end = foot,
            strokeWidth = radius * 2f,
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        drawLine(
            color = lerp(start = bardFluteLight, stop = Color.White, fraction = 0.5f),
            start = head + Offset(x = radius, y = -radius * 0.4f),
            end = foot - Offset(x = radius, y = radius * 0.4f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * 0.7f,
        )

        drawCircle(
            color = lerp(start = accentColor, stop = Color.Black, fraction = 0.3f),
            radius = radius * 0.5f,
            center = head + Offset(x = radius * 2.4f, y = -radius * 0.2f),
            alpha = alpha,
        )
        val holeCount = 5
        for (hole in 1..holeCount) {
            val fraction = 0.42f + hole / (holeCount + 1f) * 0.5f
            drawCircle(
                color = bardFluteDark,
                radius = radius * 0.42f,
                center = lerpOffset(start = head, stop = foot, fraction = fraction),
                alpha = alpha,
            )
        }

        if (airFlow > 0f) {
            drawAirWisps(origin = foot, radius = radius, airFlow = airFlow, alpha = alpha)
        }
    }
}

/** Three breathy wisps peeling off the flute's foot, sliding outward and fading as air passes. */
private fun DrawScope.drawAirWisps(origin: Offset, radius: Float, airFlow: Float, alpha: Float) {
    val reach = 34.dp.toPx()
    for (wisp in 0..2) {
        val phase = (airFlow * 1.6f + wisp * 0.33f) % 1f
        val travel = origin + Offset(x = radius + phase * reach, y = (wisp - 1) * radius * 1.3f)
        val curl = sin(x = phase * PI.toFloat() * 2f) * 4.dp.toPx()
        val wispPath =
            Path().apply {
                moveTo(x = origin.x + radius, y = travel.y - curl)
                quadraticTo(
                    x1 = (origin.x + travel.x) / 2f,
                    y1 = travel.y + curl,
                    x2 = travel.x,
                    y2 = travel.y - curl * 0.5f,
                )
            }
        val fade = sin(x = phase * PI.toFloat())
        drawPath(
            path = wispPath,
            color = Color.White,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
            alpha = alpha * fade * 0.5f,
        )
    }
}

/** The hover seats the strummed notes bloom into, spread up and out around the soundhole (dp). */
private val persuadeNoteSeats =
    listOf(
        Offset(x = -36f, y = -6f),
        Offset(x = -13f, y = -24f),
        Offset(x = 15f, y = -18f),
        Offset(x = 40f, y = 0f),
    )

private val persuadeNoteDelays = listOf(0f, 0.12f, 0.24f, 0.36f)

/**
 * The notes a strummed lute throws off (SS-207). As [enter] rises each note blooms up to its hover
 * seat around the soundhole and holds. A failed Persuade then tips them over with [fall] — they
 * tumble sideways and down, greying and fading — while a stopped one sends them up in a [flourish],
 * swept outward and up on a bright sparkle. Only one of [fall] or [flourish] ever runs.
 */
internal fun DrawScope.drawPersuadeNotes(
    anchor: Offset,
    accentColor: Color,
    grayness: Float,
    enter: Float,
    fall: Float,
    flourish: Float,
) {
    val riseIn = 12.dp.toPx()
    val fallSide = 46.dp.toPx()
    val fallDrop = 40.dp.toPx()
    val flourishRise = 40.dp.toPx()
    val flourishSpread = 26.dp.toPx()
    val noteColor =
        lerp(start = accentColor, stop = Color.White, fraction = 0.3f).grayed(fraction = grayness)

    persuadeNoteSeats.forEachIndexed { index, seat ->
        val enterPhase = phaseAfter(progress = enter, delay = persuadeNoteDelays[index])
        if (enterPhase <= 0f) return@forEachIndexed

        val hover =
            anchor + Offset(x = seat.x.dp.toPx(), y = seat.y.dp.toPx() - enterPhase * riseIn)
        val side = if (seat.x >= 0f) 1f else -1f

        val fallOffset = Offset(x = side * fall * fallSide, y = fall * fallDrop)
        val flourishOffset =
            Offset(x = side * flourish * flourishSpread, y = -flourish * flourishRise)
        val position = hover + fallOffset + flourishOffset

        val enterAlpha = (enterPhase * 3f).coerceAtMost(maximumValue = 1f)
        val isFlourishFading = flourish > 0.6f
        val flourishFade =
            if (isFlourishFading) {
                ((1f - flourish) / 0.4f).coerceIn(minimumValue = 0f, maximumValue = 1f)
            } else {
                1f
            }
        val noteAlpha = enterAlpha * (1f - fall) * flourishFade
        if (noteAlpha <= 0f) return@forEachIndexed

        val tumble = fall * 80f
        rotate(degrees = tumble, pivot = position) {
            drawMusicNote(center = position, color = noteColor, alpha = noteAlpha)
        }
        if (flourish > 0f) {
            drawSparkle(
                center = position + Offset(x = 8.dp.toPx(), y = -8.dp.toPx()),
                radius = 4.dp.toPx() * flourish,
                color = lerp(start = noteColor, stop = Color.White, fraction = 0.6f),
                alpha = noteAlpha * flourishFade,
            )
        }
    }
}

/** How many notes ride the hypnotic swirl, and the band the spiral breathes between. */
private const val SWIRL_NOTE_COUNT = 6
private val swirlInnerRadius = 20.dp
private val swirlOuterRadius = 54.dp
private const val SWIRL_TURNS = 1.4f

/**
 * The slow charmer's swirl (SS-207): notes wound onto a spiral around the flute, orbiting outward
 * as [swirl] runs and drifting up, each fading in and back out along its arc — the hypnotic coil
 * that bends the attacker onto a new target, drawn straight rather than snake-like but reading as
 * the same lull.
 */
internal fun DrawScope.drawSwirlNotes(
    anchor: Offset,
    accentColor: Color,
    swirl: Float,
    alpha: Float,
) {
    if (alpha <= 0f || swirl <= 0f) return
    val center = anchor - Offset(x = 0f, y = 8.dp.toPx())
    val inner = swirlInnerRadius.toPx()
    val outer = swirlOuterRadius.toPx()
    val noteColor = lerp(start = accentColor, stop = Color.White, fraction = 0.35f)

    for (index in 0 until SWIRL_NOTE_COUNT) {
        val stagger = index / SWIRL_NOTE_COUNT.toFloat()
        val phase =
            ((swirl - stagger * 0.3f) / (1f - 0.3f)).coerceIn(minimumValue = 0f, maximumValue = 1f)
        if (phase <= 0f) continue

        val angle = (stagger + swirl * SWIRL_TURNS) * 2f * PI.toFloat()
        val radius = inner + (outer - inner) * phase
        val position =
            center +
                Offset(
                    x = cos(x = angle) * radius,
                    y = sin(x = angle) * radius * 0.7f - phase * 10.dp.toPx(),
                )
        val fade = sin(x = phase * PI.toFloat())
        drawMusicNote(center = position, color = noteColor, alpha = alpha * fade * 0.85f)
    }
}

/** A four-point sparkle, brightest at its core, to tip a note's flourish with a glint of light. */
private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0f || radius <= 0f) return
    val arms =
        listOf(
            Offset(x = 0f, y = -1f),
            Offset(x = 1f, y = 0f),
            Offset(x = 0f, y = 1f),
            Offset(x = -1f, y = 0f),
        )
    arms.forEach { arm ->
        drawLine(
            color = color,
            start = center,
            end = center + arm * radius,
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
    }
    drawCircle(color = color, radius = radius * 0.3f, center = center, alpha = alpha)
}

/** A staggered channel's local progress once its start [delay] has passed, else zero. */
private fun phaseAfter(progress: Float, delay: Float): Float {
    if (progress <= delay) return 0f
    return ((progress - delay) / (1f - delay)).coerceIn(minimumValue = 0f, maximumValue = 1f)
}
