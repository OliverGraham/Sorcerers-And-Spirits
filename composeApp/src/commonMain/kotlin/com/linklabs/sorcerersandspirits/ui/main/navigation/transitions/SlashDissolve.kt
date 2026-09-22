package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sqrt

private const val SLASH_SEPARATION_FRACTION = 0.75f
private const val SLASH_HALVES_FADE_START = 0.55f
private const val SLASH_LINE_WIDTH_PIXELS = 12f
private const val SLASH_LINE_FADE_RATE = 2.6f

private val SLASH_LINE_COLOR = Color(red = 0.85f, green = 0.95f, blue = 1f)

@Composable
fun SlashDissolve(snapshot: ImageBitmap, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val diagonalLength = sqrt(width * width + height * height).coerceAtLeast(minimumValue = 1f)
        val normalX = height / diagonalLength
        val normalY = -width / diagonalLength
        val separation = diagonalLength * SLASH_SEPARATION_FRACTION * progress
        val halvesAlpha =
            (1f - (progress - SLASH_HALVES_FADE_START) / (1f - SLASH_HALVES_FADE_START)).coerceIn(
                minimumValue = 0f,
                maximumValue = 1f,
            )

        val topRightHalf =
            Path().apply {
                moveTo(x = 0f, y = 0f)
                lineTo(x = width, y = 0f)
                lineTo(x = width, y = height)
                close()
            }
        val bottomLeftHalf =
            Path().apply {
                moveTo(x = 0f, y = 0f)
                lineTo(x = width, y = height)
                lineTo(x = 0f, y = height)
                close()
            }

        drawSlidingHalf(
            snapshot = snapshot,
            clipRegion = topRightHalf,
            offsetX = normalX * separation,
            offsetY = normalY * separation,
            alpha = halvesAlpha,
        )
        drawSlidingHalf(
            snapshot = snapshot,
            clipRegion = bottomLeftHalf,
            offsetX = -normalX * separation,
            offsetY = -normalY * separation,
            alpha = halvesAlpha,
        )

        val lineAlpha = (1f - progress * SLASH_LINE_FADE_RATE).coerceAtLeast(minimumValue = 0f)
        val isLineVisible = lineAlpha > 0f
        if (isLineVisible) {
            drawLine(
                color = SLASH_LINE_COLOR.copy(alpha = lineAlpha),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = width, y = height),
                strokeWidth = SLASH_LINE_WIDTH_PIXELS,
            )
        }
    }
}

private fun DrawScope.drawSlidingHalf(
    snapshot: ImageBitmap,
    clipRegion: Path,
    offsetX: Float,
    offsetY: Float,
    alpha: Float,
) {
    val isHalfVisible = alpha > 0f
    if (isHalfVisible) {
        translate(left = offsetX, top = offsetY) {
            clipPath(path = clipRegion) {
                drawImage(image = snapshot, topLeft = Offset.Zero, alpha = alpha)
            }
        }
    }
}
