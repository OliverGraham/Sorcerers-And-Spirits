package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.sin
import kotlin.random.Random

private const val EFFECT_DURATION_MILLIS = 900f
private const val NANOS_PER_MILLI = 1_000_000f

// Coarser than a per-pixel sample so the particle count stays smooth on lower-end devices and iOS.
private const val SAMPLE_STEP_PIXELS = 36
private const val STRIP_COUNT = 24

// Fraction of the timeline over which the dissolve sweeps left-to-right; the remainder is the time
// any given column's particles take to drift away once they have been released.
private const val SWEEP_FRACTION = 0.45f

private const val PARTICLE_RADIUS_PIXELS = 7f
private const val PARTICLE_MIN_DRIFT_X = 40f
private const val PARTICLE_MAX_DRIFT_X = 220f
private const val PARTICLE_MIN_RISE = 120f
private const val PARTICLE_MAX_RISE = 360f
private const val PARTICLE_WOBBLE_AMPLITUDE = 26f
private const val PARTICLE_WOBBLE_MIN_FREQUENCY = 6f
private const val PARTICLE_WOBBLE_MAX_FREQUENCY = 14f
private const val FULL_TURN_RADIANS = 6.2831855f

/**
 * Plays the signature "burn to dust" pop animation: the captured outgoing screen dissolves
 * left-to-right into drifting coloured particles, revealing whatever is drawn beneath it.
 *
 * [snapshot] must be a pixel-for-pixel capture of the screen being popped, so its dimensions line
 * up one-to-one with this overlay's draw surface. [onComplete] fires once the dust has fully
 * cleared, which is the caller's cue to drop the overlay.
 */
@Composable
fun DisintegrationEffect(
    snapshot: ImageBitmap,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(snapshot) { mutableFloatStateOf(value = 0f) }
    val particles = remember(snapshot) { snapshot.toDustParticles() }
    val stripWidth =
        remember(snapshot) { (snapshot.width / STRIP_COUNT).coerceAtLeast(minimumValue = 1) }

    LaunchedEffect(snapshot) {
        var startNanos = 0L
        do {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) {
                    startNanos = frameNanos
                }
                val elapsedMillis = (frameNanos - startNanos) / NANOS_PER_MILLI
                progress =
                    (elapsedMillis / EFFECT_DURATION_MILLIS).coerceIn(
                        minimumValue = 0f,
                        maximumValue = 1f,
                    )
            }
        } while (progress < 1f)
        onComplete()
    }

    Canvas(modifier = modifier) {
        drawDissolvingStrips(snapshot = snapshot, stripWidth = stripWidth, progress = progress)
        drawDriftingParticles(particles = particles, progress = progress)
    }
}

private fun DrawScope.drawDissolvingStrips(
    snapshot: ImageBitmap,
    stripWidth: Int,
    progress: Float,
) {
    var stripIndex = 0
    while (stripIndex < STRIP_COUNT) {
        val stripStartX = stripIndex * stripWidth
        val columnProgress =
            columnLocalProgress(
                columnFraction = stripStartX.toFloat() / snapshot.width,
                progress = progress,
            )
        val stripAlpha = 1f - columnProgress
        val isStripVisible = stripAlpha > 0f
        if (isStripVisible) {
            drawImage(
                image = snapshot,
                srcOffset = IntOffset(x = stripStartX, y = 0),
                srcSize = IntSize(width = stripWidth, height = snapshot.height),
                dstOffset = IntOffset(x = stripStartX, y = 0),
                dstSize = IntSize(width = stripWidth, height = snapshot.height),
                alpha = stripAlpha,
            )
        }
        stripIndex++
    }
}

private fun DrawScope.drawDriftingParticles(particles: List<DustParticle>, progress: Float) {
    particles.forEach { particle ->
        val localProgress =
            columnLocalProgress(columnFraction = particle.columnFraction, progress = progress)
        val isReleased = localProgress > 0f && localProgress < 1f
        if (isReleased) {
            val wobble =
                sin(x = localProgress * particle.wobbleFrequency + particle.wobblePhase) *
                    PARTICLE_WOBBLE_AMPLITUDE
            val center =
                Offset(
                    x = particle.origin.x + localProgress * particle.driftX + wobble,
                    y = particle.origin.y - localProgress * particle.rise,
                )
            drawCircle(
                color = particle.color.copy(alpha = particle.color.alpha * (1f - localProgress)),
                radius = PARTICLE_RADIUS_PIXELS * (1f - localProgress * 0.5f),
                center = center,
            )
        }
    }
}

/**
 * Maps overall [progress] to how far a single column has dissolved. Columns on the left release
 * first; each then has the back half of the timeline to drift clear.
 */
private fun columnLocalProgress(columnFraction: Float, progress: Float): Float {
    val columnStart = columnFraction * SWEEP_FRACTION
    val driftWindow = 1f - SWEEP_FRACTION
    return ((progress - columnStart) / driftWindow).coerceIn(minimumValue = 0f, maximumValue = 1f)
}

private fun ImageBitmap.toDustParticles(): List<DustParticle> {
    val pixels = toPixelMap()
    val particles = mutableListOf<DustParticle>()
    var x = 0
    while (x < width) {
        var y = 0
        while (y < height) {
            val color = pixels[x, y]
            val isPaintable = color.alpha > 0f
            if (isPaintable) {
                particles.add(
                    DustParticle(
                        origin = Offset(x = x.toFloat(), y = y.toFloat()),
                        columnFraction = x.toFloat() / width,
                        color = color,
                        driftX =
                            randomBetween(min = PARTICLE_MIN_DRIFT_X, max = PARTICLE_MAX_DRIFT_X),
                        rise = randomBetween(min = PARTICLE_MIN_RISE, max = PARTICLE_MAX_RISE),
                        wobbleFrequency =
                            randomBetween(
                                min = PARTICLE_WOBBLE_MIN_FREQUENCY,
                                max = PARTICLE_WOBBLE_MAX_FREQUENCY,
                            ),
                        wobblePhase = Random.nextFloat() * FULL_TURN_RADIANS,
                    )
                )
            }
            y += SAMPLE_STEP_PIXELS
        }
        x += SAMPLE_STEP_PIXELS
    }
    return particles
}

private fun randomBetween(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)

private class DustParticle(
    val origin: Offset,
    val columnFraction: Float,
    val color: Color,
    val driftX: Float,
    val rise: Float,
    val wobbleFrequency: Float,
    val wobblePhase: Float,
)
