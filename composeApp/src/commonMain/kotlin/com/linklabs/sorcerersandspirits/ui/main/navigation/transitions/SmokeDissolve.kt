package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

private const val SMOKE_STEP_PIXELS = 48
private const val SMOKE_POSITION_JITTER = 30f
private const val SMOKE_BASE_RADIUS_MIN = 22f
private const val SMOKE_BASE_RADIUS_MAX = 46f
private const val SMOKE_EXPAND_SCALE = 2.4f
private const val SMOKE_RISE_PIXELS = 340f
private const val SMOKE_DRIFT_PIXELS = 90f
private const val SMOKE_PEAK_ALPHA = 0.5f
private const val SMOKE_IMAGE_FADE_RATE = 1.5f
private const val HALF_TURN_RADIANS = 3.1415927f

private val SMOKE_COLOR = Color(red = 0.62f, green = 0.63f, blue = 0.66f)

@Composable
fun SmokeDissolve(snapshot: ImageBitmap, progress: Float, modifier: Modifier = Modifier) {
    val puffs = remember(key1 = snapshot) { snapshot.toSmokePuffs() }

    Canvas(modifier = modifier) {
        val imageAlpha = (1f - progress * SMOKE_IMAGE_FADE_RATE).coerceAtLeast(minimumValue = 0f)
        val isImageVisible = imageAlpha > 0f
        if (isImageVisible) {
            drawImage(image = snapshot, topLeft = Offset.Zero, alpha = imageAlpha)
        }
        drawSmokePuffs(puffs = puffs, progress = progress)
    }
}

private fun DrawScope.drawSmokePuffs(puffs: List<SmokePuff>, progress: Float) {
    puffs.forEach { puff ->
        val puffProgress = (progress + puff.delay).coerceIn(minimumValue = 0f, maximumValue = 1f)
        val radius = puff.baseRadius * (1f + puffProgress * SMOKE_EXPAND_SCALE)
        val alpha = sin(x = puffProgress * HALF_TURN_RADIANS) * SMOKE_PEAK_ALPHA
        val isPuffVisible = alpha > 0f
        if (isPuffVisible) {
            val center =
                Offset(
                    x = puff.origin.x + puff.driftX * puffProgress,
                    y = puff.origin.y - SMOKE_RISE_PIXELS * puffProgress,
                )
            drawCircle(
                color = SMOKE_COLOR.copy(alpha = alpha),
                radius = radius,
                center = center,
            )
        }
    }
}

private fun ImageBitmap.toSmokePuffs(): List<SmokePuff> {
    val puffs = mutableListOf<SmokePuff>()
    var x = 0

    while (x < width) {
        var y = 0
        while (y < height) {
            puffs.add(
                SmokePuff(
                    origin =
                    Offset(
                        x = x + randomBetween(min = -SMOKE_POSITION_JITTER, max = SMOKE_POSITION_JITTER),
                        y = y + randomBetween(min = -SMOKE_POSITION_JITTER, max = SMOKE_POSITION_JITTER),
                    ),
                    baseRadius = randomBetween(min = SMOKE_BASE_RADIUS_MIN, max = SMOKE_BASE_RADIUS_MAX),
                    driftX = randomBetween(min = -SMOKE_DRIFT_PIXELS, max = SMOKE_DRIFT_PIXELS),
                    delay = randomBetween(min = 0f, max = 0.15f),
                ),
            )
            y += SMOKE_STEP_PIXELS
        }
        x += SMOKE_STEP_PIXELS
    }

    return puffs
}

private class SmokePuff(
    val origin: Offset,
    val baseRadius: Float,
    val driftX: Float,
    val delay: Float,
)
