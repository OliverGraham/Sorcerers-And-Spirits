package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val encoreDark = Color(color = 0xFF05040A)
private val encoreSpotlight = Color(color = 0xFFFFF4D2)
private val micChromeLight = Color(color = 0xFFEDEFF5)
private val micChromeMid = Color(color = 0xFF9AA0AC)
private val micChromeDark = Color(color = 0xFF35383F)
private val encoreAsh = Color(color = 0xFF8C8A85)
private val encoreBooTint = Color(color = 0xFF9DA7C4)

private fun Color.grayed(fraction: Float): Color =
    lerp(start = this, stop = encoreAsh, fraction = fraction)

internal class EncoreCrowdMember(
    val centerXFraction: Float,
    val centerYFraction: Float,
    val depth: Float,
    val avatarIndex: Int,
    val phase: Float,
)

internal fun encoreCrowd(): List<EncoreCrowdMember> {
    val rows = 7
    val members = mutableListOf<EncoreCrowdMember>()
    var seed = 0
    fun next(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 16) and 0x7fff) / 32767f
    }
    for (row in 0 until rows) {
        val depth = 1f - row / (rows - 1f)
        val rowY = 0.12f + row / (rows - 1f) * 0.72f
        val perRow = 7 + row
        for (column in 0 until perRow) {
            val slot = (column + 0.5f) / perRow
            val jitterX = (next() - 0.5f) * (0.9f / perRow)
            val centerX = slot + jitterX
            val isFrontCenterGap = row >= rows - 2 && centerX > 0.36f && centerX < 0.64f
            if (isFrontCenterGap) continue
            members.add(
                EncoreCrowdMember(
                    centerXFraction = centerX,
                    centerYFraction = rowY + (next() - 0.5f) * 0.04f,
                    depth = depth,
                    avatarIndex = (row * perRow + column) % 8,
                    phase = next(),
                ),
            )
        }
    }
    return members
}

internal fun DrawScope.drawEncoreScrim(darkness: Float) {
    if (darkness <= 0f) return
    drawRect(color = encoreDark, alpha = darkness * 0.94f)
}

internal fun DrawScope.drawEncoreCrowd(
    crowd: List<EncoreCrowdMember>,
    avatars: List<ImageBitmap>,
    sway: Float,
    reaction: Float,
    isSuccessful: Boolean,
    darkness: Float,
) {
    if (darkness <= 0f) return
    val nearHeight = size.minDimension * 0.16f
    crowd.forEach { member ->
        val avatar = avatars[member.avatarIndex]
        val faceHeight = nearHeight * (0.42f + (1f - member.depth) * 0.58f)
        val faceWidth = faceHeight * (avatar.width.toFloat() / avatar.height.toFloat())

        val swayAngle = sway * 2f * PI.toFloat() + member.phase * 2f * PI.toFloat()
        val ambientBob = sin(x = swayAngle) * faceHeight * 0.05f
        val ambientLean = cos(x = swayAngle) * faceWidth * 0.06f

        val reactionBob =
            if (isSuccessful) {
                -abs(x = sin(x = swayAngle * 1.6f)) * faceHeight * 0.35f * reaction
            } else {
                0f
            }
        val reactionShake =
            if (isSuccessful) 0f else sin(x = swayAngle * 3.2f) * faceWidth * 0.22f * reaction

        val centerX = member.centerXFraction * size.width + ambientLean + reactionShake
        val centerY = member.centerYFraction * size.height + ambientBob + reactionBob

        val spotlightSpill = 1f - abs(x = member.centerXFraction - 0.5f) * 1.4f
        val baseGlow = 0.28f + spotlightSpill.coerceAtLeast(minimumValue = 0f) * 0.32f
        val moodGlow = if (isSuccessful) reaction * 0.4f else -reaction * 0.14f
        val brightness = (baseGlow + moodGlow).coerceIn(minimumValue = 0.12f, maximumValue = 0.95f)

        val moodTint = if (isSuccessful) encoreSpotlight else encoreBooTint
        val faceColor = lerp(start = Color.Black, stop = moodTint, fraction = brightness)

        drawImage(
            image = avatar,
            dstOffset =
            IntOffset(
                x = (centerX - faceWidth / 2f).roundToInt(),
                y = (centerY - faceHeight / 2f).roundToInt(),
            ),
            dstSize = IntSize(width = faceWidth.roundToInt(), height = faceHeight.roundToInt()),
            alpha = darkness * (0.55f + (1f - member.depth) * 0.3f),
            colorFilter = ColorFilter.tint(color = faceColor, blendMode = BlendMode.Modulate),
        )
    }
}

internal fun DrawScope.drawEncoreStage(alpha: Float) {
    if (alpha <= 0f) return
    val stageTop = size.height * 0.82f
    drawRect(
        brush =
        Brush.verticalGradient(
            colors = listOf(Color(color = 0xFF17141F), encoreDark),
            startY = stageTop,
            endY = size.height,
        ),
        topLeft = Offset(x = 0f, y = stageTop),
        size = Size(width = size.width, height = size.height - stageTop),
        alpha = alpha,
    )
    drawLine(
        color = encoreSpotlight,
        start = Offset(x = size.width * 0.2f, y = stageTop),
        end = Offset(x = size.width * 0.8f, y = stageTop),
        strokeWidth = 2f,
        alpha = alpha * 0.25f,
    )
}

private fun DrawScope.micHeadCenter(): Offset = Offset(
    x = size.width * 0.5f,
    y =
    size.height * 0.58f,
)

internal fun DrawScope.drawEncoreSpotlight(spotlight: Float) {
    if (spotlight <= 0f) return
    val head = micHeadCenter()
    val apex = Offset(x = head.x, y = -size.height * 0.05f)
    val baseY = size.height * 0.9f
    val spread = size.width * 0.32f

    val cone =
        Path().apply {
            moveTo(x = apex.x, y = apex.y)
            lineTo(x = head.x - spread, y = baseY)
            lineTo(x = head.x + spread, y = baseY)
            close()
        }
    drawPath(
        path = cone,
        brush =
        Brush.verticalGradient(
            colors =
            listOf(
                encoreSpotlight.copy(alpha = 0.16f),
                encoreSpotlight.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            startY = apex.y,
            endY = baseY,
        ),
        alpha = spotlight,
    )
    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(encoreSpotlight.copy(alpha = 0.35f), Color.Transparent),
            center = head,
            radius = spread * 1.1f,
        ),
        radius = spread * 1.1f,
        center = head,
        alpha = spotlight,
    )
}

private fun DrawScope.micHeadRadius(): Float = size.minDimension * 0.06f

internal fun DrawScope.drawMicrophone(glint: Float, grayness: Float, alpha: Float) {
    if (alpha <= 0f) return
    val head = micHeadCenter()
    val radius = micHeadRadius()
    val baseCenter = Offset(x = head.x, y = size.height * 0.88f)

    drawMicStand(head = head, radius = radius, baseCenter = baseCenter, alpha = alpha)
    drawMicBase(center = baseCenter, radius = radius, alpha = alpha)
    drawMicHead(head = head, radius = radius, glint = glint, grayness = grayness, alpha = alpha)
}

private fun DrawScope.drawMicStand(head: Offset, radius: Float, baseCenter: Offset, alpha: Float) {
    val neckTop = head + Offset(x = 0f, y = radius * 0.85f)
    val halfWidth = radius * 0.26f
    drawRect(
        brush =
        Brush.horizontalGradient(
            colors = listOf(micChromeDark, micChromeLight, micChromeMid, micChromeDark),
            startX = head.x - halfWidth,
            endX = head.x + halfWidth,
        ),
        topLeft = Offset(x = head.x - halfWidth, y = neckTop.y),
        size = Size(width = halfWidth * 2f, height = baseCenter.y - neckTop.y),
        alpha = alpha,
    )
    drawCircle(color = micChromeDark, radius = halfWidth * 1.5f, center = neckTop, alpha = alpha)
}

private fun DrawScope.drawMicBase(center: Offset, radius: Float, alpha: Float) {
    val discWidth = radius * 2.2f
    val discHeight = radius * 0.7f
    drawOval(
        brush =
        Brush.radialGradient(
            colors = listOf(micChromeLight, micChromeMid, micChromeDark),
            center = center - Offset(x = discWidth * 0.15f, y = discHeight * 0.2f),
            radius = discWidth * 0.7f,
        ),
        topLeft = Offset(x = center.x - discWidth / 2f, y = center.y - discHeight / 2f),
        size = Size(width = discWidth, height = discHeight),
        alpha = alpha,
    )
}

private fun DrawScope.drawMicHead(
    head: Offset,
    radius: Float,
    glint: Float,
    grayness: Float,
    alpha: Float,
) {
    val highlight = Offset(x = head.x - radius * 0.4f, y = head.y - radius * 0.4f)
    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(micChromeLight, micChromeMid, micChromeDark),
            center = highlight,
            radius = radius * 1.5f,
        ),
        radius = radius,
        center = head,
        alpha = alpha,
    )

    val grilleColor = micChromeDark.copy(alpha = 0.5f)
    val bands = 5
    for (band in 1 until bands) {
        val fraction = band / bands.toFloat()
        val y = head.y - radius + fraction * radius * 2f
        val half = radius * sin(x = fraction * PI.toFloat())
        drawLine(
            color = grilleColor,
            start = Offset(x = head.x - half, y = y),
            end = Offset(x = head.x + half, y = y),
            strokeWidth = radius * 0.08f,
            alpha = alpha * 0.7f,
        )
    }

    val glintAngle = glint * 2f * PI.toFloat()
    val glintCenter =
        head +
            Offset(x = cos(x = glintAngle) * radius * 0.4f, y = sin(x = glintAngle) * radius * 0.4f)
    drawCircle(
        color = lerp(start = Color.White, stop = encoreSpotlight, fraction = grayness),
        radius = radius * 0.22f,
        center = glintCenter,
        alpha = alpha * 0.75f,
    )

    drawCircle(
        color = micChromeDark,
        radius = radius,
        center = head,
        style = Stroke(width = radius * 0.12f),
        alpha = alpha * 0.6f,
    )
}

private val encoreNoteSeats =
    listOf(
        Offset(x = -46f, y = -8f),
        Offset(x = -30f, y = -34f),
        Offset(x = -8f, y = -18f),
        Offset(x = 20f, y = -36f),
        Offset(x = 40f, y = -10f),
        Offset(x = 8f, y = -46f),
    )

private val encoreNoteDelays = listOf(0f, 0.1f, 0.18f, 0.26f, 0.34f, 0.42f)

internal fun DrawScope.drawEncoreMusic(
    accentColor: Color,
    roll: Float,
    grayness: Float,
    alpha: Float,
) {
    if (alpha <= 0f || roll <= 0f) return
    val head = micHeadCenter()
    val noteColor =
        lerp(start = accentColor, stop = Color.White, fraction = 0.35f).grayed(fraction = grayness)

    drawSoundRings(center = head, roll = roll, color = noteColor, alpha = alpha)

    encoreNoteSeats.forEachIndexed { index, seat ->
        val phase = phaseAfter(progress = roll, delay = encoreNoteDelays[index])
        if (phase <= 0f) return@forEachIndexed

        val rise = phase * 40.dp.toPx()
        val drift = Offset(x = seat.x.dp.toPx(), y = seat.y.dp.toPx()) * (0.5f + phase)
        val position = head + drift - Offset(x = 0f, y = rise)
        val fade =
            when {
                phase < 0.2f -> phase / 0.2f
                phase > 0.7f -> ((1f - phase) / 0.3f).coerceIn(minimumValue = 0f, maximumValue = 1f)
                else -> 1f
            }
        drawMusicNote(center = position, color = noteColor, alpha = alpha * fade)
    }
}

private fun DrawScope.drawSoundRings(center: Offset, roll: Float, color: Color, alpha: Float) {
    val maxRadius = size.minDimension * 0.22f
    for (ring in 0..2) {
        val phase = (roll * 1.4f - ring * 0.28f).coerceIn(minimumValue = 0f, maximumValue = 1f)
        if (phase <= 0f) continue
        drawCircle(
            color = color,
            radius = maxRadius * phase,
            center = center,
            style = Stroke(width = 2.dp.toPx() * (1f - phase)),
            alpha = alpha * (1f - phase) * 0.5f,
        )
    }
}

internal fun DrawScope.drawEncoreVignette(darkness: Float) {
    if (darkness <= 0f) return
    drawRect(
        brush =
        Brush.radialGradient(
            colors = listOf(Color.Transparent, encoreDark),
            center = Offset(x = size.width * 0.5f, y = size.height * 0.55f),
            radius = size.maxDimension * 0.62f,
        ),
        alpha = darkness * 0.9f,
    )
}

private fun phaseAfter(progress: Float, delay: Float): Float {
    if (progress <= delay) return 0f
    return ((progress - delay) / (1f - delay)).coerceIn(minimumValue = 0f, maximumValue = 1f)
}
