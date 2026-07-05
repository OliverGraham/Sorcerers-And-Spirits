package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelBright
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelShade

/** The length of the Duelist's rapier blade, shared by the twin-sword strike and Prepare Attack. */
internal val rapierBladeLength = 88.dp

/**
 * A slender rapier in local coordinates: tip at the origin leading the cut, a thin fencing blade
 * holding its width then tapering to a sharp point, in bright steel with a specular glint down its
 * length and a white flash at the tip. The hilt carries the class-color accent — curved quillons, a
 * swept knuckle bow curling around the hand, the grip, and the pommel.
 */
internal fun DrawScope.drawSwordShape(accentColor: Color, alpha: Float) {
    val bladeLength = rapierBladeLength.toPx()
    val bladeHalfWidth = 2.6.dp.toPx()
    val taperStart = -bladeLength * 0.8f
    val guardY = -bladeLength
    val quillonHalfWidth = 15.dp.toPx()
    val gripHalfWidth = 3.dp.toPx()
    val gripLength = 17.dp.toPx()
    val gripTopY = guardY - gripLength
    val pommelY = gripTopY - 3.dp.toPx()

    val blade =
        Path().apply {
            moveTo(x = 0f, y = 0f)
            lineTo(x = -bladeHalfWidth, y = taperStart)
            lineTo(x = -bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = taperStart)
            close()
        }
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(Color.White, steelBright, steelShade),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 0f, y = guardY),
            ),
        alpha = alpha,
    )

    drawLine(
        color = Color.White,
        start = Offset(x = 0f, y = taperStart),
        end = Offset(x = 0f, y = guardY * 0.12f),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.85f,
    )

    drawCircle(
        color = Color.White,
        radius = 2.dp.toPx(),
        center = Offset(x = 0f, y = 0f),
        alpha = alpha * 0.9f,
    )

    val quillons =
        Path().apply {
            moveTo(x = -quillonHalfWidth, y = guardY - 5.dp.toPx())
            cubicTo(
                x1 = -quillonHalfWidth * 0.3f,
                y1 = guardY + 5.dp.toPx(),
                x2 = quillonHalfWidth * 0.3f,
                y2 = guardY + 5.dp.toPx(),
                x3 = quillonHalfWidth,
                y3 = guardY - 5.dp.toPx(),
            )
        }
    drawPath(
        path = quillons,
        color = accentColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )

    val knuckleBow =
        Path().apply {
            moveTo(x = quillonHalfWidth, y = guardY - 5.dp.toPx())
            cubicTo(
                x1 = quillonHalfWidth + 7.dp.toPx(),
                y1 = guardY - gripLength * 0.4f,
                x2 = gripHalfWidth + 5.dp.toPx(),
                y2 = pommelY + 3.dp.toPx(),
                x3 = 0f,
                y3 = pommelY,
            )
        }
    drawPath(
        path = knuckleBow,
        color = accentColor,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -gripHalfWidth, y = gripTopY),
        size = Size(width = gripHalfWidth * 2, height = gripLength),
        cornerRadius = CornerRadius(x = gripHalfWidth),
        alpha = alpha,
    )

    drawCircle(
        color = accentColor,
        radius = 4.dp.toPx(),
        center = Offset(x = 0f, y = pommelY),
        alpha = alpha,
    )
}
