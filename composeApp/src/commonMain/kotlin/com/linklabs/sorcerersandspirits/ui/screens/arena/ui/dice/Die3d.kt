package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val DIE_SIDE_DP = 64

// The die lives in a tray larger than itself so it has room to be thrown and bounce around inside
// it (SS-113). On release the die is given a random velocity and integrated frame-by-frame,
// ricocheting off the tray walls and slowing under friction until it comes to rest.
private const val TRAY_WIDTH_DP = 116
private const val TRAY_HEIGHT_DP = 88

// Random throw speed range, in dp per second.
private const val MIN_THROW_SPEED_DP = 600f
private const val MAX_THROW_SPEED_DP = 1150f

// Fraction of speed the die keeps after bouncing off a wall; the rest is lost to the impact.
private const val WALL_RESTITUTION = 0.62f

// Velocity decays by e^(-FRICTION_PER_SECOND * dt) each frame, so the die coasts to a stop.
private const val FRICTION_PER_SECOND = 1.8f

// At or below this speed (dp/sec) the die is treated as at rest and stops bouncing.
private const val REST_SPEED_DP = 45f

// How fast the die tumbles as it travels: degrees of spin per dp of travel.
private const val ROLL_DEGREES_PER_DP = 0.7f

// The die's half-extent (as a fraction of its side) used as the bounce radius against the walls.
private const val DIE_RADIUS_FRACTION = 0.34f

// A long frame after a stall is clamped to this many seconds so the die can't tunnel through a
// wall.
private const val MAX_FRAME_SECONDS = 0.033f

// Camera sits this many cube-half-widths back along +Z; larger means a gentler perspective.
private const val CAMERA_DISTANCE = 10f
private const val CUBE_HALF_FRACTION = 0.22f

// A fixed view looking down on the die from a three-quarter angle, the way you read a die resting
// on
// a table: the rolled face sits on top and reads clearly, with a front and side face visible below.
private const val VIEW_TILT_X_DEGREES = 55f
private const val VIEW_TILT_Y_DEGREES = 30f

// Pip centres on a face, as a fraction of the face half-width.
private const val PIP_GRID = 0.5f
private const val PIP_RADIUS_FRACTION = 0.17f

private const val EDGE_STROKE_PX = 1.5f

// Faces angled away from the camera darken to this fraction of full brightness for depth shading.
private const val SHADE_MIN = 0.55f

// Each detected shake kicks the held die with a random impulse in this speed range (dp/sec); a few
// shakes stack up into a lively bounce, while friction bleeds it back down between them.
private const val MIN_SHAKE_IMPULSE_DP = 450f
private const val MAX_SHAKE_IMPULSE_DP = 950f

// Once the die stops bouncing it rights itself over this long so the rolled face reads up.
private const val RIGHTING_MILLIS = 450

// After a roll settles, the die glides from wherever it stopped back to its resting home over this
// long (SS-149); only when a [restCenter] is given (the screen-spanning roll, not the tray).
private const val GLIDE_HOME_MILLIS = 480

private val DIE_BODY_ENABLED = Color(color = 0xFFF4ECDA)
private val DIE_SHADOW_ENABLED = Color(color = 0xFF9C8F70)
private val DIE_PIP_ENABLED = Color(color = 0xFF24201A)
private val DIE_BODY_DISABLED = Color(color = 0xFF5A5A5A)
private val DIE_SHADOW_DISABLED = Color(color = 0xFF3A3A3A)
private val DIE_PIP_DISABLED = Color(color = 0xFF8A8A8A)
private val DIE_EDGE = Color(color = 0x33000000)

/**
 * A real 3D die (SS-077 prototype): an actual cube projected with perspective and back-face
 * culling, drawn entirely in Compose [Canvas]. While [isRolling] it bounces around the tray and
 * each [shakeTick] (one per phone shake) kicks it with a fresh impulse, so shaking the device
 * throws the dice around live (SS-112); a tiny per-frame physics simulation [runDiePhysics]
 * ricochets it off the tray walls, tumbling and slowing under friction (SS-113). On release the
 * motion the shaking built up carries through and settles, and when [value] changes because any
 * player rolled, a fresh throw reveals it. Only once it comes to rest does it right itself so the
 * face carrying [value] reads up — the way a physical die settles after it stops rolling. The
 * landing value is exact because the righting target is the orientation that puts that face up,
 * independent of the chaotic path the die took to get there.
 */
@Composable
internal fun Die3d(
    value: Int,
    isRolling: Boolean,
    enabled: Boolean,
    shakeTick: Int,
    modifier: Modifier = Modifier,
    trayWidth: Dp = TRAY_WIDTH_DP.dp,
    trayHeight: Dp = TRAY_HEIGHT_DP.dp,
    restCenter: Offset? = null,
) {
    val density = LocalDensity.current
    val trayWidthPx = with(density) { trayWidth.toPx() }
    val trayHeightPx = with(density) { trayHeight.toPx() }
    // Where the die sits at rest: a screen-spanning roll homes to the given [restCenter]; the small
    // tray just centers it. Gliding back to home only happens for the former.
    val homeCenter = restCenter ?: Offset(x = trayWidthPx / 2f, y = trayHeightPx / 2f)
    val shouldGlideHome = restCenter != null
    val restAngles = remember(value) { faceAngles(value = value) }
    val angleX = remember { Animatable(initialValue = restAngles.first) }
    val angleY = remember { Animatable(initialValue = restAngles.second) }
    val center = remember {
        Animatable(initialValue = homeCenter, typeConverter = Offset.VectorConverter)
    }
    // The die rests on its current face the first time it lays out, then tumbles for every change
    // after that: while held, and on any value change once a roll (by any player) lands (SS-112).
    var isFirstLayout by remember { mutableStateOf(value = true) }
    // The value the die last settled on, so a later change can be told apart from a release.
    var previousValue by remember { mutableStateOf(value = value) }
    // The die's live velocity is handed from the held shake-physics loop to the release settle, so
    // releasing continues the motion the shaking built up rather than starting over.
    val velocity = remember { mutableStateOf(value = Offset.Zero) }
    // Read inside the physics loop so each new shake is seen without restarting the loop.
    val currentShakeTick by rememberUpdatedState(newValue = shakeTick)

    LaunchedEffect(key1 = isRolling, key2 = value) {
        val target = faceAngles(value = value)
        val restSpeed = REST_SPEED_DP * density.density
        if (isRolling) {
            isFirstLayout = false
            var lastShakeTick = currentShakeTick
            // While held the die bounces around the tray, and every shake of the phone kicks it
            // with a fresh impulse; friction bleeds the motion away between shakes (SS-112). The
            // loop never settles on its own — it ends when the hold releases and this effect is
            // cancelled, leaving [velocity] for the release settle to continue from.
            runDiePhysics(
                center = center,
                angleX = angleX,
                angleY = angleY,
                trayWidthPx = trayWidthPx,
                trayHeightPx = trayHeightPx,
                density = density.density,
                startVelocity = randomThrowVelocity(density = density.density),
                onVelocityChanged = { latest -> velocity.value = latest },
                nextImpulse = {
                    val tick = currentShakeTick
                    val hasShaken = tick != lastShakeTick
                    if (hasShaken) {
                        lastShakeTick = tick
                        randomShakeImpulse(density = density.density)
                    } else {
                        Offset.Zero
                    }
                },
                isAtRest = { false },
            )
        } else if (isFirstLayout) {
            isFirstLayout = false
            previousValue = value
            angleX.snapTo(targetValue = target.first)
            angleY.snapTo(targetValue = target.second)
            center.snapTo(targetValue = homeCenter)
        } else {
            val hasValueChanged = value != previousValue
            previousValue = value
            val heldVelocity = velocity.value
            velocity.value = Offset.Zero

            val settleVelocity =
                when {
                    // A disabled die never tumbles: it only glides back to its home and rights
                    // itself while the active die rolls (SS-158), even if it was mid-flight when
                    // the
                    // roll that follows turned it off.
                    !enabled -> Offset.Zero

                    // Released after shaking: keep the momentum the shaking built up.
                    heldVelocity.getDistance() > restSpeed -> heldVelocity

                    // A new value landed (this player's result or another player's roll): reveal it
                    // with a fresh throw across the tray.
                    hasValueChanged -> randomThrowVelocity(density = density.density)

                    // Released without rolling: nothing to throw, just right the face below.
                    else -> Offset.Zero
                }

            if (settleVelocity != Offset.Zero) {
                runDiePhysics(
                    center = center,
                    angleX = angleX,
                    angleY = angleY,
                    trayWidthPx = trayWidthPx,
                    trayHeightPx = trayHeightPx,
                    density = density.density,
                    startVelocity = settleVelocity,
                    onVelocityChanged = {},
                    nextImpulse = { Offset.Zero },
                    isAtRest = { speed -> speed <= restSpeed },
                )
            }

            coroutineScope {
                if (shouldGlideHome) {
                    launch {
                        center.animateTo(
                            targetValue = homeCenter,
                            animationSpec =
                                tween(
                                    durationMillis = GLIDE_HOME_MILLIS,
                                    easing = LinearOutSlowInEasing,
                                ),
                        )
                    }
                }
                launch {
                    angleX.animateTo(
                        targetValue =
                            rightingAngle(current = angleX.value, faceAngle = target.first),
                        animationSpec =
                            tween(durationMillis = RIGHTING_MILLIS, easing = LinearOutSlowInEasing),
                    )
                }
                angleY.animateTo(
                    targetValue = rightingAngle(current = angleY.value, faceAngle = target.second),
                    animationSpec =
                        tween(durationMillis = RIGHTING_MILLIS, easing = LinearOutSlowInEasing),
                )
            }
        }
    }

    // Keep a resting die parked on its home as the home is (re)measured, without disturbing a roll.
    LaunchedEffect(key1 = homeCenter) { if (!isRolling) center.snapTo(targetValue = homeCenter) }

    Canvas(modifier = modifier.size(width = trayWidth, height = trayHeight)) {
        drawDie(
            angleX = angleX.value,
            angleY = angleY.value,
            center = center.value,
            enabled = enabled,
        )
    }
}

/**
 * Integrates the die bouncing around the tray, one frame at a time. Starting from [center]'s
 * current spot and [startVelocity], each frame adds [nextImpulse] (a shake's kick, or zero), loses
 * speed to friction, reverses (losing energy) whenever it reaches a wall, and tumbles
 * [angleX]/[angleY] in proportion to how far it moved. [onVelocityChanged] reports the running
 * velocity so a caller can hand it from one phase to the next, and the loop ends when [isAtRest]
 * accepts the current speed — the held phase passes a predicate that never rests, so it runs until
 * the effect is cancelled. [density] (px per dp) converts the dp-based tuning into the pixel space
 * the animatables work in.
 */
private suspend fun runDiePhysics(
    center: Animatable<Offset, *>,
    angleX: Animatable<Float, *>,
    angleY: Animatable<Float, *>,
    trayWidthPx: Float,
    trayHeightPx: Float,
    density: Float,
    startVelocity: Offset,
    onVelocityChanged: (Offset) -> Unit,
    nextImpulse: () -> Offset,
    isAtRest: (Float) -> Boolean,
) {
    val radius = DIE_SIDE_DP * density * DIE_RADIUS_FRACTION
    val spanX = radius..(trayWidthPx - radius)
    val spanY = radius..(trayHeightPx - radius)
    val degreesPerPx = ROLL_DEGREES_PER_DP / density

    var velocity = startVelocity
    var position = center.value
    var previousFrame = withFrameNanos { it }

    while (true) {
        val now = withFrameNanos { it }
        val seconds = ((now - previousFrame) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
        previousFrame = now

        velocity += nextImpulse()
        velocity *= exp(x = -FRICTION_PER_SECOND * seconds)
        var nextX = position.x + velocity.x * seconds
        var nextY = position.y + velocity.y * seconds
        var velocityX = velocity.x
        var velocityY = velocity.y
        val hasHitSideWall = nextX !in spanX
        val hasHitTopOrBottomWall = nextY !in spanY
        if (hasHitSideWall) {
            nextX = nextX.coerceIn(range = spanX)
            velocityX = -velocityX * WALL_RESTITUTION
        }
        if (hasHitTopOrBottomWall) {
            nextY = nextY.coerceIn(range = spanY)
            velocityY = -velocityY * WALL_RESTITUTION
        }
        velocity = Offset(x = velocityX, y = velocityY)
        position = Offset(x = nextX, y = nextY)

        onVelocityChanged(velocity)
        center.snapTo(targetValue = position)
        angleX.snapTo(targetValue = angleX.value + velocityY * seconds * degreesPerPx)
        angleY.snapTo(targetValue = angleY.value + velocityX * seconds * degreesPerPx)

        if (isAtRest(velocity.getDistance())) return
    }
}

/** A throw across the tray in a random direction at a random speed, in pixels per second. */
private fun randomThrowVelocity(density: Float): Offset {
    val angle = Random.nextFloat() * 2f * PI
    val speed =
        (MIN_THROW_SPEED_DP + Random.nextFloat() * (MAX_THROW_SPEED_DP - MIN_THROW_SPEED_DP)) *
            density
    return Offset(x = cos(x = angle) * speed, y = sin(x = angle) * speed)
}

/** A single shake's kick in a random direction, in pixels per second. */
private fun randomShakeImpulse(density: Float): Offset {
    val angle = Random.nextFloat() * 2f * PI
    val speed =
        (MIN_SHAKE_IMPULSE_DP +
            Random.nextFloat() * (MAX_SHAKE_IMPULSE_DP - MIN_SHAKE_IMPULSE_DP)) * density
    return Offset(x = cos(x = angle) * speed, y = sin(x = angle) * speed)
}

private fun DrawScope.drawDie(angleX: Float, angleY: Float, center: Offset, enabled: Boolean) {
    val bodyColor = if (enabled) DIE_BODY_ENABLED else DIE_BODY_DISABLED
    val shadowColor = if (enabled) DIE_SHADOW_ENABLED else DIE_SHADOW_DISABLED
    val pipColor = if (enabled) DIE_PIP_ENABLED else DIE_PIP_DISABLED

    val rotation =
        rotationX(degrees = VIEW_TILT_X_DEGREES) *
            rotationY(degrees = VIEW_TILT_Y_DEGREES) *
            rotationX(degrees = angleX) *
            rotationY(degrees = angleY)

    val halfWidth = DIE_SIDE_DP.dp.toPx() * CUBE_HALF_FRACTION

    CUBE_FACES.forEach { face ->
        val rotatedNormal = rotation * face.normal
        val isFacingCamera = rotatedNormal.z > 0f
        if (!isFacingCamera) return@forEach

        val corners =
            CORNER_SIGNS.map { (signU, signV) ->
                val corner = face.center + face.axisU * signU + face.axisV * signV
                project(point = rotation * corner, center = center, halfWidth = halfWidth)
            }
        val facePath =
            Path().apply {
                moveTo(x = corners[0].x, y = corners[0].y)
                corners.drop(n = 1).forEach { lineTo(x = it.x, y = it.y) }
                close()
            }
        val brightness = SHADE_MIN + (1f - SHADE_MIN) * rotatedNormal.z
        drawPath(
            path = facePath,
            color = lerp(start = shadowColor, stop = bodyColor, fraction = brightness),
        )
        drawPath(path = facePath, color = DIE_EDGE, style = Stroke(width = EDGE_STROKE_PX))

        pipLocalOffsets(value = face.value).forEach { (localU, localV) ->
            val pipPoint = rotation * (face.center + face.axisU * localU + face.axisV * localV)
            val perspective = CAMERA_DISTANCE / (CAMERA_DISTANCE - pipPoint.z)
            drawCircle(
                color = pipColor,
                radius = halfWidth * PIP_RADIUS_FRACTION * perspective,
                center = project(point = pipPoint, center = center, halfWidth = halfWidth),
            )
        }
    }
}

private fun project(point: Vec3, center: Offset, halfWidth: Float): Offset {
    val perspective = CAMERA_DISTANCE / (CAMERA_DISTANCE - point.z)
    return Offset(
        x = center.x + point.x * halfWidth * perspective,
        y = center.y - point.y * halfWidth * perspective,
    )
}

/**
 * The angle nearest [current] that presents [faceAngle] (modulo a full turn), reached by the
 * shortest rotation in either direction. Used to right the die onto its rolled face after it stops
 * bouncing, so it ticks into place rather than spinning a long way around.
 */
private fun rightingAngle(current: Float, faceAngle: Float): Float {
    val delta = (((faceAngle - current) % 360f) + 540f) % 360f - 180f
    return current + delta
}

/**
 * The (rotationX, rotationY) that brings the face carrying [value] to the top (+Y) before the fixed
 * view tilt is applied, so the rolled value is the face you read. Opposite faces sum to 7.
 */
private fun faceAngles(value: Int): Pair<Float, Float> =
    when (value) {
        1 -> 0f to 0f
        2 -> -90f to 0f
        3 -> 90f to 90f
        4 -> -90f to 90f
        5 -> 90f to 0f
        6 -> 180f to 0f
        else -> 0f to 0f
    }

/** Pip centres for a face, in face-local (u, v) fractions of the half-width. */
private fun pipLocalOffsets(value: Int): List<Pair<Float, Float>> {
    val g = PIP_GRID
    return when (value) {
        1 -> listOf(0f to 0f)
        2 -> listOf(-g to g, g to -g)
        3 -> listOf(-g to g, 0f to 0f, g to -g)
        4 -> listOf(-g to -g, g to -g, -g to g, g to g)
        5 -> listOf(-g to -g, g to -g, 0f to 0f, -g to g, g to g)
        6 -> listOf(-g to -g, -g to 0f, -g to g, g to -g, g to 0f, g to g)
        else -> emptyList()
    }
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x = x + other.x, y = y + other.y, z = z + other.z)

    operator fun times(scalar: Float) = Vec3(x = x * scalar, y = y * scalar, z = z * scalar)
}

private class Mat3(private val m: FloatArray) {
    operator fun times(o: Mat3): Mat3 {
        val result = FloatArray(size = 9)
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                var sum = 0f
                for (k in 0 until 3) sum += m[row * 3 + k] * o.m[k * 3 + col]
                result[row * 3 + col] = sum
            }
        }
        return Mat3(m = result)
    }

    operator fun times(v: Vec3) =
        Vec3(
            x = m[0] * v.x + m[1] * v.y + m[2] * v.z,
            y = m[3] * v.x + m[4] * v.y + m[5] * v.z,
            z = m[6] * v.x + m[7] * v.y + m[8] * v.z,
        )
}

private fun rotationX(degrees: Float): Mat3 {
    val radians = degrees * PI_OVER_180
    val c = cos(x = radians)
    val s = sin(x = radians)
    return Mat3(m = floatArrayOf(1f, 0f, 0f, 0f, c, -s, 0f, s, c))
}

private fun rotationY(degrees: Float): Mat3 {
    val radians = degrees * PI_OVER_180
    val c = cos(x = radians)
    val s = sin(x = radians)
    return Mat3(m = floatArrayOf(c, 0f, s, 0f, 1f, 0f, -s, 0f, c))
}

private const val PI_OVER_180 = 0.017453292f
private const val PI = 3.1415927f
private const val NANOS_PER_SECOND = 1_000_000_000f

private val CORNER_SIGNS = listOf(1f to 1f, 1f to -1f, -1f to -1f, -1f to 1f)

private class CubeFace(val value: Int, val normal: Vec3, val axisU: Vec3, val axisV: Vec3) {
    val center: Vec3
        get() = normal
}

private val CUBE_FACES =
    listOf(
        CubeFace(
            value = 2,
            normal = Vec3(0f, 0f, 1f),
            axisU = Vec3(1f, 0f, 0f),
            axisV = Vec3(0f, 1f, 0f),
        ),
        CubeFace(
            value = 5,
            normal = Vec3(0f, 0f, -1f),
            axisU = Vec3(-1f, 0f, 0f),
            axisV = Vec3(0f, 1f, 0f),
        ),
        CubeFace(
            value = 3,
            normal = Vec3(1f, 0f, 0f),
            axisU = Vec3(0f, 0f, -1f),
            axisV = Vec3(0f, 1f, 0f),
        ),
        CubeFace(
            value = 4,
            normal = Vec3(-1f, 0f, 0f),
            axisU = Vec3(0f, 0f, 1f),
            axisV = Vec3(0f, 1f, 0f),
        ),
        CubeFace(
            value = 1,
            normal = Vec3(0f, 1f, 0f),
            axisU = Vec3(1f, 0f, 0f),
            axisV = Vec3(0f, 0f, -1f),
        ),
        CubeFace(
            value = 6,
            normal = Vec3(0f, -1f, 0f),
            axisU = Vec3(1f, 0f, 0f),
            axisV = Vec3(0f, 0f, 1f),
        ),
    )
