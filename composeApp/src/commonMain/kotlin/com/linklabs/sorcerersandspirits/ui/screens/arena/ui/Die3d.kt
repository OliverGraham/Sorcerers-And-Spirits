package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DIE_SIDE_DP = 64

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

private const val SPIN_X_STEP_DEGREES = 540f
private const val SPIN_Y_STEP_DEGREES = 720f
private const val SPIN_STEP_MILLIS = 600
private const val SETTLE_MILLIS = 900
private const val SETTLE_EXTRA_TURNS_X = 1
private const val SETTLE_EXTRA_TURNS_Y = 2
private const val MIN_FORWARD_DEGREES = 1f

private val DIE_BODY_ENABLED = Color(color = 0xFFF4ECDA)
private val DIE_SHADOW_ENABLED = Color(color = 0xFF9C8F70)
private val DIE_PIP_ENABLED = Color(color = 0xFF24201A)
private val DIE_BODY_DISABLED = Color(color = 0xFF5A5A5A)
private val DIE_SHADOW_DISABLED = Color(color = 0xFF3A3A3A)
private val DIE_PIP_DISABLED = Color(color = 0xFF8A8A8A)
private val DIE_EDGE = Color(color = 0x33000000)

/**
 * A real 3D die (SS-077 prototype): an actual cube projected with perspective and back-face
 * culling, drawn entirely in Compose [Canvas]. While [isRolling] it tumbles; on release it
 * decelerates and settles so the face carrying [value] comes to rest on top — the way you read a
 * physical die. The landing is exact because the settle target is the orientation that puts that
 * face up, not a snap after a canned animation.
 */
@Composable
internal fun Die3d(
    value: Int,
    isRolling: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val restAngles = remember(value) { faceAngles(value = value) }
    val angleX = remember { Animatable(initialValue = restAngles.first) }
    val angleY = remember { Animatable(initialValue = restAngles.second) }
    var hasRolledOnce by remember { mutableStateOf(value = false) }

    LaunchedEffect(key1 = isRolling, key2 = value) {
        val target = faceAngles(value = value)
        if (isRolling) {
            hasRolledOnce = true
            coroutineScope {
                launch {
                    while (isActive) {
                        angleX.animateTo(
                            targetValue = angleX.value + SPIN_X_STEP_DEGREES,
                            animationSpec =
                                tween(durationMillis = SPIN_STEP_MILLIS, easing = LinearEasing),
                        )
                    }
                }
                launch {
                    while (isActive) {
                        angleY.animateTo(
                            targetValue = angleY.value + SPIN_Y_STEP_DEGREES,
                            animationSpec =
                                tween(durationMillis = SPIN_STEP_MILLIS, easing = LinearEasing),
                        )
                    }
                }
            }
        } else if (!hasRolledOnce) {
            angleX.snapTo(targetValue = target.first)
            angleY.snapTo(targetValue = target.second)
        } else {
            val settleX =
                settleAngle(
                    current = angleX.value,
                    faceAngle = target.first,
                    extraTurns = SETTLE_EXTRA_TURNS_X,
                )
            val settleY =
                settleAngle(
                    current = angleY.value,
                    faceAngle = target.second,
                    extraTurns = SETTLE_EXTRA_TURNS_Y,
                )
            coroutineScope {
                launch {
                    angleX.animateTo(
                        targetValue = settleX,
                        animationSpec =
                            tween(durationMillis = SETTLE_MILLIS, easing = LinearOutSlowInEasing),
                    )
                }
                angleY.animateTo(
                    targetValue = settleY,
                    animationSpec =
                        tween(durationMillis = SETTLE_MILLIS, easing = LinearOutSlowInEasing),
                )
            }
        }
    }

    Canvas(modifier = modifier.size(size = DIE_SIDE_DP.dp)) {
        drawDie(angleX = angleX.value, angleY = angleY.value, enabled = enabled)
    }
}

private fun DrawScope.drawDie(angleX: Float, angleY: Float, enabled: Boolean) {
    val bodyColor = if (enabled) DIE_BODY_ENABLED else DIE_BODY_DISABLED
    val shadowColor = if (enabled) DIE_SHADOW_ENABLED else DIE_SHADOW_DISABLED
    val pipColor = if (enabled) DIE_PIP_ENABLED else DIE_PIP_DISABLED

    val rotation =
        rotationX(degrees = VIEW_TILT_X_DEGREES) *
            rotationY(degrees = VIEW_TILT_Y_DEGREES) *
            rotationX(degrees = angleX) *
            rotationY(degrees = angleY)

    val center = Offset(x = size.width / 2f, y = size.height / 2f)
    val halfWidth = size.minDimension * CUBE_HALF_FRACTION

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
 * The accumulated angle to ease to so the die ends presenting [faceAngle] (modulo a full turn),
 * after rolling forward through [extraTurns] more rotations for a natural decelerating finish.
 */
private fun settleAngle(current: Float, faceAngle: Float, extraTurns: Int): Float {
    val normalized = ((faceAngle % 360f) + 360f) % 360f
    var candidate = floor(current / 360f) * 360f + normalized
    while (candidate < current + MIN_FORWARD_DEGREES) candidate += 360f
    return candidate + extraTurns * 360f
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
