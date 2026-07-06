package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The stained wood of the Bard's snare shell and the paler wood of the stick (SS-196). */
internal val bardShellLight = Color(color = 0xFFB07C3E)
internal val bardShellDark = Color(color = 0xFF6B4522)
internal val bardStickLight = Color(color = 0xFFD8AE6A)
internal val bardStickDark = Color(color = 0xFF8A6230)

/** The white batter head, and the grey it shades to as the stick drives a dip into it. */
internal val bardHeadWhite = Color(color = 0xFFF6F3EA)
internal val bardHeadShade = Color(color = 0xFFBFB9A6)

/** The drum's diameter and shell depth, and how far the batter head sinks under a full strike. */
private val drumRadiusX = 46.dp
private val drumHeadRadiusY = 13.dp
private val drumShellHeight = 40.dp
internal val drumMaxDip = 6.dp

/**
 * The Bard's snare drum (SS-196): a wooden shell banded top and bottom by bard-colored counterhoops
 * with tension rods between them, capped by a white batter head. [depress] sinks the head into a
 * shallow bowl as the stick strikes and lets it bounce back as the stick lifts; [scale] pops the
 * whole drum in; [alpha] fades it. The head dips within the rigid hoop, which holds its rest line.
 */
internal fun DrawScope.drawSnareDrum(
    center: Offset,
    depress: Float,
    scale: Float,
    alpha: Float,
    rimColor: Color,
) {
    if (alpha <= 0f) return
    scale(scale = scale, pivot = center) {
        val radiusX = drumRadiusX.toPx()
        val headRadiusY = drumHeadRadiusY.toPx()
        val shellHeight = drumShellHeight.toPx()
        val topY = center.y
        val bottomY = center.y + shellHeight

        drawShell(
            center = center,
            radiusX = radiusX,
            headRadiusY = headRadiusY,
            topY = topY,
            bottomY = bottomY,
            alpha = alpha,
        )
        drawTensionRods(
            center = center,
            radiusX = radiusX,
            topY = topY,
            bottomY = bottomY,
            rimColor = rimColor,
            alpha = alpha,
        )
        drawHoop(
            center = Offset(x = center.x, y = bottomY),
            radiusX = radiusX,
            headRadiusY = headRadiusY,
            rimColor = rimColor,
            alpha = alpha,
            frontArcOnly = true,
        )
        drawBatterHead(
            center = center,
            radiusX = radiusX,
            headRadiusY = headRadiusY,
            topY = topY,
            depress = depress,
            alpha = alpha,
        )
        drawHoop(
            center = Offset(x = center.x, y = topY),
            radiusX = radiusX,
            headRadiusY = headRadiusY,
            rimColor = rimColor,
            alpha = alpha,
            frontArcOnly = false,
        )
    }
}

/** How far a control point sits off an ellipse's axis to bow a cubic into a passable half-arc. */
private const val ELLIPSE_ARC_CONTROL = 1.33f

/** The wooden shell: a tapered cylinder whose front edges bow toward the viewer, with grain. */
private fun DrawScope.drawShell(
    center: Offset,
    radiusX: Float,
    headRadiusY: Float,
    topY: Float,
    bottomY: Float,
    alpha: Float,
) {
    val bow = headRadiusY * ELLIPSE_ARC_CONTROL
    val shell =
        Path().apply {
            moveTo(x = center.x - radiusX, y = topY)
            lineTo(x = center.x - radiusX, y = bottomY)
            cubicTo(
                x1 = center.x - radiusX * 0.55f,
                y1 = bottomY + bow,
                x2 = center.x + radiusX * 0.55f,
                y2 = bottomY + bow,
                x3 = center.x + radiusX,
                y3 = bottomY,
            )
            lineTo(x = center.x + radiusX, y = topY)
            cubicTo(
                x1 = center.x + radiusX * 0.55f,
                y1 = topY + bow,
                x2 = center.x - radiusX * 0.55f,
                y2 = topY + bow,
                x3 = center.x - radiusX,
                y3 = topY,
            )
            close()
        }
    drawPath(
        path = shell,
        brush =
            Brush.verticalGradient(
                colors = listOf(bardShellLight, bardShellDark),
                startY = topY,
                endY = bottomY + bow,
            ),
        alpha = alpha,
    )
    drawWoodGrain(center = center, radiusX = radiusX, topY = topY, bottomY = bottomY, alpha = alpha)
}

/** Faint vertical staves down the shell so the wood reads as barrel-built rather than flat. */
private fun DrawScope.drawWoodGrain(
    center: Offset,
    radiusX: Float,
    topY: Float,
    bottomY: Float,
    alpha: Float,
) {
    val staves = listOf(-0.62f, -0.3f, 0f, 0.3f, 0.62f)
    staves.forEach { fraction ->
        val x = center.x + radiusX * fraction
        drawLine(
            color = bardShellDark,
            start = Offset(x = x, y = topY + 3.dp.toPx()),
            end = Offset(x = x, y = bottomY - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            alpha = alpha * 0.35f,
        )
    }
}

/** The bright vertical tension rods running the bard-colored hoops together down the shell. */
private fun DrawScope.drawTensionRods(
    center: Offset,
    radiusX: Float,
    topY: Float,
    bottomY: Float,
    rimColor: Color,
    alpha: Float,
) {
    val rodColor = lerp(start = rimColor, stop = Color.White, fraction = 0.2f)
    val rods = listOf(-0.78f, -0.44f, -0.12f, 0.2f, 0.52f, 0.82f)
    rods.forEach { fraction ->
        val x = center.x + radiusX * fraction
        drawLine(
            color = rodColor,
            start = Offset(x = x, y = topY + 2.dp.toPx()),
            end = Offset(x = x, y = bottomY - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * 0.85f,
        )
    }
}

/**
 * A rigid bard-colored counterhoop banding one end of the shell. [frontArcOnly] draws just the near
 * half of the ring for the bottom hoop, so its back edge cannot show through the opaque shell.
 */
private fun DrawScope.drawHoop(
    center: Offset,
    radiusX: Float,
    headRadiusY: Float,
    rimColor: Color,
    alpha: Float,
    frontArcOnly: Boolean,
) {
    val hoopDark = lerp(start = rimColor, stop = Color.Black, fraction = 0.35f)
    val brush =
        Brush.verticalGradient(
            colors = listOf(rimColor, hoopDark),
            startY = center.y - headRadiusY,
            endY = center.y + headRadiusY,
        )
    val topLeft = Offset(x = center.x - radiusX, y = center.y - headRadiusY)
    val size = Size(width = radiusX * 2f, height = headRadiusY * 2f)
    val stroke = Stroke(width = 5.dp.toPx())

    if (frontArcOnly) {
        drawArc(
            brush = brush,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = stroke,
            alpha = alpha,
        )
    } else {
        drawOval(brush = brush, topLeft = topLeft, size = size, style = stroke, alpha = alpha)
    }
}

/** The white batter head, domed at rest and pressed into a shaded bowl as [depress] rises. */
private fun DrawScope.drawBatterHead(
    center: Offset,
    radiusX: Float,
    headRadiusY: Float,
    topY: Float,
    depress: Float,
    alpha: Float,
) {
    val headRadiusX = radiusX * 0.9f
    val headHalfHeight = headRadiusY * 0.86f
    val dipY = topY + depress * drumMaxDip.toPx()
    val headCenter = Offset(x = center.x, y = dipY)

    drawOval(
        brush =
            Brush.radialGradient(
                colors = listOf(bardHeadWhite, bardHeadShade),
                center = Offset(x = headCenter.x, y = headCenter.y - headHalfHeight * 0.4f),
                radius = headRadiusX,
            ),
        topLeft = Offset(x = headCenter.x - headRadiusX, y = headCenter.y - headHalfHeight),
        size = Size(width = headRadiusX * 2f, height = headHalfHeight * 2f),
        alpha = alpha,
    )

    if (depress > 0f) {
        drawOval(
            brush =
                Brush.radialGradient(
                    colors = listOf(bardHeadShade, Color.Transparent),
                    center = headCenter,
                    radius = headRadiusX * 0.7f,
                ),
            topLeft = Offset(x = headCenter.x - headRadiusX, y = headCenter.y - headHalfHeight),
            size = Size(width = headRadiusX * 2f, height = headHalfHeight * 2f),
            alpha = alpha * depress * 0.7f,
        )
    }
}

/**
 * The reach from the fulcrum out to the bead, and the short butt that overhangs behind the hand.
 */
internal val drumstickLength = 84.dp
private val drumstickButt = 12.dp

/**
 * A wooden drumstick pivoting on its [fulcrum] — the drummer's grip — at [angleRadians] off level,
 * measured with down positive. The bead leads at the far end and a short butt overhangs behind the
 * grip; the shaft tapers from butt to bead with a wood-grain gradient and a highlight down its top.
 */
internal fun DrawScope.drawDrumstick(fulcrum: Offset, angleRadians: Float, alpha: Float) {
    if (alpha <= 0f) return
    val direction = Offset(x = cos(x = angleRadians), y = sin(x = angleRadians))
    val perpendicular = Offset(x = -direction.y, y = direction.x)
    val bead = fulcrum + direction * drumstickLength.toPx()
    val butt = fulcrum - direction * drumstickButt.toPx()

    val beadHalf = 2.4.dp.toPx()
    val buttHalf = 3.2.dp.toPx()
    val shaft =
        Path().apply {
            moveTo(x = bead.x + perpendicular.x * beadHalf, y = bead.y + perpendicular.y * beadHalf)
            lineTo(x = butt.x + perpendicular.x * buttHalf, y = butt.y + perpendicular.y * buttHalf)
            lineTo(x = butt.x - perpendicular.x * buttHalf, y = butt.y - perpendicular.y * buttHalf)
            lineTo(x = bead.x - perpendicular.x * beadHalf, y = bead.y - perpendicular.y * beadHalf)
            close()
        }
    drawPath(
        path = shaft,
        brush =
            Brush.linearGradient(
                colors = listOf(bardStickLight, bardStickDark),
                start = bead,
                end = butt,
            ),
        alpha = alpha,
    )
    drawLine(
        color = lerp(start = bardStickLight, stop = Color.White, fraction = 0.4f),
        start = bead - perpendicular * (beadHalf * 0.4f),
        end = butt - perpendicular * (buttHalf * 0.4f),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.6f,
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(bardStickLight, bardStickDark),
                center = bead - perpendicular * 1.5.dp.toPx(),
                radius = 5.dp.toPx(),
            ),
        radius = 4.6.dp.toPx(),
        center = bead,
        alpha = alpha,
    )
}

/** How far apart in the travel the three sound rings ripple out, and their arc reach. */
private const val SOUND_RING_STAGGER = 0.24f
private val soundRingNearRadius = 11.dp
private val soundRingFarRadius = 30.dp

/**
 * Three ripples of sound bowing forward off the struck head and carrying down the [direction]
 * toward the opponent (SS-196). Each ring leaves the drum a beat after the last, swelling and
 * fading as it travels [reach] pixels, so the burst reads as directional sound rather than a
 * spreading circle.
 */
internal fun DrawScope.drawSoundWaves(
    origin: Offset,
    direction: Offset,
    reach: Float,
    progress: Float,
    color: Color,
    alpha: Float,
) {
    if (alpha <= 0f) return
    val perpendicular = Offset(x = -direction.y, y = direction.x)
    val nearDistance = soundRingNearRadius.toPx() + 22.dp.toPx()

    for (ring in 0..2) {
        val ringProgress = progress - ring * SOUND_RING_STAGGER
        if (ringProgress <= 0f || ringProgress >= 1f) continue

        val distance = nearDistance + (reach - nearDistance) * ringProgress
        val position = origin + direction * distance
        val radius =
            soundRingNearRadius.toPx() +
                (soundRingFarRadius.toPx() - soundRingNearRadius.toPx()) * ringProgress
        val bow = radius * 0.62f
        val ringAlpha = alpha * sin(x = ringProgress * PI.toFloat()) * (1f - ring * 0.12f)

        val arc =
            Path().apply {
                moveTo(
                    x = position.x + perpendicular.x * radius,
                    y = position.y + perpendicular.y * radius,
                )
                quadraticTo(
                    x1 = position.x + direction.x * bow,
                    y1 = position.y + direction.y * bow,
                    x2 = position.x - perpendicular.x * radius,
                    y2 = position.y - perpendicular.y * radius,
                )
            }
        drawPath(
            path = arc,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = ringAlpha,
        )
    }
}

/** How far above the drum the notes climb, and how far the trio spreads while rising. */
private val musicNoteRise = 78.dp
private val musicNoteDrift = 26.dp

/** The horizontal seat of each rising note and the beat it enters on, spreading the trio out. */
private val musicNoteSeats = listOf(-0.6f, 0.15f, 0.75f)
private val musicNoteDelays = listOf(0f, 0.14f, 0.28f)

/**
 * The music that answers a landed strike (SS-196): a few bard-colored notes rising and drifting off
 * the top of the drum, fading as they climb. Only drawn on a hit; a miss leaves the air silent.
 */
internal fun DrawScope.drawMusicNotes(anchor: Offset, progress: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    musicNoteSeats.forEachIndexed { index, seat ->
        val phase = (progress - musicNoteDelays[index]) / (1f - musicNoteDelays[index])
        if (phase <= 0f || phase >= 1f) return@forEachIndexed

        val rise = phase * musicNoteRise.toPx()
        val drift =
            seat * musicNoteDrift.toPx() + sin(x = phase * PI.toFloat() * 1.5f) * 5.dp.toPx()
        val noteCenter = Offset(x = anchor.x + drift, y = anchor.y - rise)
        drawMusicNote(
            center = noteCenter,
            color = color,
            alpha = alpha * musicNoteFade(phase = phase),
        )
    }
}

/** A note fades in as it leaves the head and fades back out as it climbs away. */
private fun musicNoteFade(phase: Float): Float =
    when {
        phase < 0.2f -> phase / 0.2f
        phase > 0.6f -> ((1f - phase) / 0.4f).coerceIn(minimumValue = 0f, maximumValue = 1f)
        else -> 1f
    }

/** One eighth note: a slanted filled head, a stem up its right side, and a small flag. */
private fun DrawScope.drawMusicNote(center: Offset, color: Color, alpha: Float) {
    val headRadiusX = 5.dp.toPx()
    val headRadiusY = 3.6.dp.toPx()
    drawOval(
        color = color,
        topLeft = Offset(x = center.x - headRadiusX, y = center.y - headRadiusY),
        size = Size(width = headRadiusX * 2f, height = headRadiusY * 2f),
        alpha = alpha,
    )

    val stemTop = Offset(x = center.x + headRadiusX * 0.85f, y = center.y - 18.dp.toPx())
    drawLine(
        color = color,
        start = Offset(x = center.x + headRadiusX * 0.85f, y = center.y),
        end = stemTop,
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha,
    )
    val flag =
        Path().apply {
            moveTo(x = stemTop.x, y = stemTop.y)
            quadraticTo(
                x1 = stemTop.x + 7.dp.toPx(),
                y1 = stemTop.y + 4.dp.toPx(),
                x2 = stemTop.x + 4.dp.toPx(),
                y2 = stemTop.y + 11.dp.toPx(),
            )
        }
    drawPath(
        path = flag,
        color = color,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )
}
