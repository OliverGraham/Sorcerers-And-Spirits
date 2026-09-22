package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.sin
import kotlin.random.Random

private const val SAMPLE_STEP_PIXELS = 30
private const val STRIP_COUNT = 24
private const val SWEEP_FRACTION = 0.45f

private const val PARTICLE_RADIUS_PIXELS = 7f
private const val PARTICLE_MIN_DRIFT_X = 40f
private const val PARTICLE_MAX_DRIFT_X = 260f
private const val PARTICLE_MIN_RISE = 150f
private const val PARTICLE_MAX_RISE = 480f
private const val PARTICLE_GRAVITY_PIXELS = 150f
private const val PARTICLE_WOBBLE_AMPLITUDE = 34f
private const val PARTICLE_WOBBLE_MIN_FREQUENCY = 6f
private const val PARTICLE_WOBBLE_MAX_FREQUENCY = 14f
private const val FULL_TURN_RADIANS = 6.2831855f

private val EMBER_GLOW_COLOR = Color(red = 1f, green = 0.45f, blue = 0.12f)
private const val EMBER_GLOW_ALPHA = 0.6f
private const val EMBER_GLOW_SCALE = 2.6f
private const val EMBER_COOL_RATE = 1.6f

private val BURN_EDGE_COLOR = Color(red = 1f, green = 0.55f, blue = 0.18f)
private const val BURN_EDGE_ALPHA = 0.55f
private const val BURN_EDGE_WINDOW = 0.14f

@Composable
fun DisintegrateDissolve(snapshot: ImageBitmap, progress: Float, modifier: Modifier = Modifier) {
    val particles = remember(key1 = snapshot) { snapshot.toDustParticles() }
    val stripWidth =
        remember(key1 = snapshot) { (snapshot.width / STRIP_COUNT).coerceAtLeast(minimumValue = 1) }

    Canvas(modifier = modifier) {
        drawDissolvingStrips(snapshot = snapshot, stripWidth = stripWidth, progress = progress)
        drawBurnFront(snapshot = snapshot, stripWidth = stripWidth, progress = progress)
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

private fun DrawScope.drawBurnFront(snapshot: ImageBitmap, stripWidth: Int, progress: Float) {
    var stripIndex = 0
    while (stripIndex < STRIP_COUNT) {
        val stripStartX = stripIndex * stripWidth
        val columnProgress =
            columnLocalProgress(
                columnFraction = stripStartX.toFloat() / snapshot.width,
                progress = progress,
            )
        val isAtFront = columnProgress > 0f && columnProgress < BURN_EDGE_WINDOW
        if (isAtFront) {
            val frontIntensity = 1f - columnProgress / BURN_EDGE_WINDOW
            drawRect(
                color = BURN_EDGE_COLOR.copy(alpha = frontIntensity * BURN_EDGE_ALPHA),
                topLeft = Offset(x = stripStartX.toFloat(), y = 0f),
                size = Size(width = stripWidth.toFloat(), height = snapshot.height.toFloat()),
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
            val gravityFall = PARTICLE_GRAVITY_PIXELS * localProgress * localProgress
            val center =
                Offset(
                    x = particle.origin.x + localProgress * particle.driftX + wobble,
                    y = particle.origin.y - localProgress * particle.rise + gravityFall,
                )
            val life = 1f - localProgress
            val heat = (1f - localProgress * EMBER_COOL_RATE).coerceAtLeast(minimumValue = 0f)
            val hasHeat = heat > 0f
            if (hasHeat) {
                drawCircle(
                    color = EMBER_GLOW_COLOR.copy(alpha = heat * EMBER_GLOW_ALPHA),
                    radius = PARTICLE_RADIUS_PIXELS * EMBER_GLOW_SCALE * heat,
                    center = center,
                )
            }
            drawCircle(
                color = particle.color.copy(alpha = particle.color.alpha * life),
                radius = PARTICLE_RADIUS_PIXELS * (1f - localProgress * 0.5f),
                center = center,
            )
        }
    }
}

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
                    ),
                )
            }
            y += SAMPLE_STEP_PIXELS
        }
        x += SAMPLE_STEP_PIXELS
    }

    return particles
}

private class DustParticle(
    val origin: Offset,
    val columnFraction: Float,
    val color: Color,
    val driftX: Float,
    val rise: Float,
    val wobbleFrequency: Float,
    val wobblePhase: Float,
)
