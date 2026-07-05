package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The spectral greens the Necromancer's summoned scythe is lit with (SS-195). */
internal val necroBladeCore = Color(color = 0xFFEFFFC9)
internal val necroBladeGreen = Color(color = 0xFF9BE23A)
internal val necroGlowGreen = Color(color = 0xFF66E048)
internal val necroSnathGreen = Color(color = 0xFF3F5A11)

/**
 * The Necromancer's scythe in local coordinates (SS-195): a manifested magic weapon, not a physical
 * one — a long snath trailing down from the origin and a long, slim, curved crescent blade leading
 * off it, drawn as green energy rather than wood and steel. The tip sits at the origin, the snath
 * falls away behind it, and a green glow radiates off the blade. [glow] fades that aura in as the
 * weapon is summoned; [alpha] fades the whole construct in and out.
 */
internal fun DrawScope.drawScytheShape(glow: Float, alpha: Float) {
    val blade = scytheBladePath()
    drawScytheGlow(blade = blade, glow = glow, alpha = alpha)
    drawSnath(alpha = alpha)
    drawBladeFill(blade = blade, alpha = alpha)
}

/** The tip that leads the reap, at the origin of the local frame. */
private val scytheTip = Offset(x = 0f, y = 0f)

/** Where the blade meets the snath, down and to the side of the tip. */
private fun DrawScope.scytheNeck() = Offset(x = 16.dp.toPx(), y = 91.dp.toPx())

/**
 * The inner cutting edge, sampled from the base of the blade up toward the tip. A reaper's edge is
 * faintly uneven; the tiny steps between these points give it a very subtle jag without breaking
 * the long crescent line.
 */
private fun DrawScope.scytheEdgePoints(): List<Offset> =
    listOf(
        Offset(x = 30.dp.toPx(), y = 64.dp.toPx()),
        Offset(x = 31.5.dp.toPx(), y = 62.dp.toPx()),
        Offset(x = 26.dp.toPx(), y = 40.dp.toPx()),
        Offset(x = 27.dp.toPx(), y = 38.dp.toPx()),
        Offset(x = 14.dp.toPx(), y = 16.dp.toPx()),
    )

/** The base of the blade on the cutting side, where the crescent springs off the snath. */
private fun DrawScope.scytheNeckInner() = Offset(x = 12.dp.toPx(), y = 90.dp.toPx())

/** The base of the blade on the spine side, the wide end of the taper. */
private fun DrawScope.scytheNeckOuter() = Offset(x = 20.dp.toPx(), y = 92.dp.toPx())

/**
 * The crescent outline: a long convex spine bowing off the tip and back to the blade's base, closed
 * by the subtly jagged cutting edge running back up to the tip. Slim throughout, tapering to a
 * point.
 */
private fun DrawScope.scytheBladePath(): Path =
    Path().apply {
        moveTo(x = scytheTip.x, y = scytheTip.y)
        cubicTo(
            x1 = 42.dp.toPx(),
            y1 = 16.dp.toPx(),
            x2 = 40.dp.toPx(),
            y2 = 62.dp.toPx(),
            x3 = scytheNeckOuter().x,
            y3 = scytheNeckOuter().y,
        )
        lineTo(x = scytheNeckInner().x, y = scytheNeckInner().y)
        scytheEdgePoints().forEach { point -> lineTo(x = point.x, y = point.y) }
        close()
    }

/** The blade body, a bright energy fill deepening down the crescent with a hot edge on the cut. */
private fun DrawScope.drawBladeFill(blade: Path, alpha: Float) {
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(necroBladeCore, necroBladeGreen, necroSnathGreen),
                start = scytheTip,
                end = scytheNeck(),
            ),
        alpha = alpha,
    )

    val edge =
        Path().apply {
            moveTo(x = scytheNeckInner().x, y = scytheNeckInner().y)
            scytheEdgePoints().forEach { point -> lineTo(x = point.x, y = point.y) }
            lineTo(x = scytheTip.x, y = scytheTip.y)
        }
    drawPath(
        path = edge,
        color = necroBladeCore,
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.9f,
    )
}

/** The snath: a long S-curved handle of dim green energy falling away behind the blade. */
private fun DrawScope.drawSnath(alpha: Float) {
    val snath =
        Path().apply {
            moveTo(x = scytheNeck().x, y = scytheNeck().y - 2.dp.toPx())
            cubicTo(
                x1 = 6.dp.toPx(),
                y1 = 128.dp.toPx(),
                x2 = 16.dp.toPx(),
                y2 = 158.dp.toPx(),
                x3 = 28.dp.toPx(),
                y3 = 196.dp.toPx(),
            )
        }
    drawPath(
        path = snath,
        color = necroSnathGreen,
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )
    drawPath(
        path = snath,
        color = necroBladeGreen,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha * 0.7f,
    )
}

/**
 * The scythe's aura: the blade wrapped in a green energy glow — a wide soft halo layered off its
 * outline down to a tight bright rim, plus an inner bloom filling the blade — so the weapon reads
 * as summoned energy radiating off the blade rather than a solid object.
 */
private fun DrawScope.drawScytheGlow(blade: Path, glow: Float, alpha: Float) {
    if (glow <= 0f) return
    val auraAlpha = glow * alpha

    drawPath(
        path = blade,
        color = necroGlowGreen,
        style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.16f,
    )
    drawPath(
        path = blade,
        color = necroGlowGreen,
        style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.24f,
    )
    drawPath(
        path = blade,
        color = necroBladeGreen,
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.35f,
    )
    drawPath(path = blade, color = necroGlowGreen, alpha = auraAlpha * 0.3f)
}
