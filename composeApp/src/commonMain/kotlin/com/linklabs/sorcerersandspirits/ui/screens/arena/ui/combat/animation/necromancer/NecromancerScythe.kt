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

internal val necroBladeCore = Color(color = 0xFFEFFFC9)
internal val necroBladeGreen = Color(color = 0xFF9BE23A)
internal val necroGlowGreen = Color(color = 0xFF66E048)
internal val necroSnathGreen = Color(color = 0xFF3F5A11)

internal data class ScythePalette(
    val core: Color,
    val blade: Color,
    val glow: Color,
    val snath: Color,
)

internal val necroScythePalette =
    ScythePalette(
        core = necroBladeCore,
        blade = necroBladeGreen,
        glow = necroGlowGreen,
        snath = necroSnathGreen,
    )

internal val illusionistScythePalette =
    ScythePalette(
        core = Color(color = 0xFFD6EAFF),
        blade = Color(color = 0xFF387CD3),
        glow = Color(color = 0xFF4C8FE0),
        snath = Color(color = 0xFF1D406D),
    )

internal fun DrawScope.drawScytheShape(palette: ScythePalette, glow: Float, alpha: Float) {
    val blade = scytheBladePath()
    drawScytheGlow(blade = blade, palette = palette, glow = glow, alpha = alpha)
    drawSnath(palette = palette, alpha = alpha)
    drawBladeFill(blade = blade, palette = palette, alpha = alpha)
}

private val scytheTip = Offset(x = 0f, y = 0f)

private fun DrawScope.scytheNeck() = Offset(x = 16.dp.toPx(), y = 91.dp.toPx())

private fun DrawScope.scytheEdgePoints(): List<Offset> =
    listOf(
        Offset(x = 30.dp.toPx(), y = 64.dp.toPx()),
        Offset(x = 31.5.dp.toPx(), y = 62.dp.toPx()),
        Offset(x = 26.dp.toPx(), y = 40.dp.toPx()),
        Offset(x = 27.dp.toPx(), y = 38.dp.toPx()),
        Offset(x = 14.dp.toPx(), y = 16.dp.toPx()),
    )

private fun DrawScope.scytheNeckInner() = Offset(x = 12.dp.toPx(), y = 90.dp.toPx())

private fun DrawScope.scytheNeckOuter() = Offset(x = 20.dp.toPx(), y = 92.dp.toPx())

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

private fun DrawScope.drawBladeFill(blade: Path, palette: ScythePalette, alpha: Float) {
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(palette.core, palette.blade, palette.snath),
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
        color = palette.core,
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.9f,
    )
}

private fun DrawScope.drawSnath(palette: ScythePalette, alpha: Float) {
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
        color = palette.snath,
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )
    drawPath(
        path = snath,
        color = palette.blade,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha * 0.7f,
    )
}

private fun DrawScope.drawScytheGlow(
    blade: Path,
    palette: ScythePalette,
    glow: Float,
    alpha: Float,
) {
    if (glow <= 0f) return
    val auraAlpha = glow * alpha

    drawPath(
        path = blade,
        color = palette.glow,
        style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.16f,
    )
    drawPath(
        path = blade,
        color = palette.glow,
        style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.24f,
    )
    drawPath(
        path = blade,
        color = palette.blade,
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = auraAlpha * 0.35f,
    )
    drawPath(path = blade, color = palette.glow, alpha = auraAlpha * 0.3f)
}
