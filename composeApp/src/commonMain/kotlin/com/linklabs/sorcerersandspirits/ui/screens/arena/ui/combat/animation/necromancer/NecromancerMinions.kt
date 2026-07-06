package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp

/** The three things a Summon Minion roll can raise from the portal (SS-205); Fizzle raises none. */
enum class MinionKind {
    None,
    Skeleton,
    Golem,
}

private val boneWhite = Color(color = 0xFFF2F0E6)
private val boneShade = Color(color = 0xFFB7B5AA)
private val clayBody = Color(color = 0xFF9B6A43)
private val clayShade = Color(color = 0xFF6E4A2E)
private val clayLight = Color(color = 0xFFBB8A5F)
private val portalGrayCore = Color(color = 0xFFBFC2BB)
private val portalGrayEdge = Color(color = 0xFF565A53)

/**
 * How flat the portal disc reads on the ground: a circle squashed to this fraction of its height.
 */
private const val PORTAL_FLATTEN = 0.42f

/**
 * The glowing portal at the summoner's feet (SS-205): a green energy disc lying flat on the ground,
 * a bright core fading out through the Necromancer's greens to a dark rim. On a fizzle it drains to
 * lifeless gray as [gray] rises, right before it closes.
 */
internal fun DrawScope.drawSummonPortal(
    center: Offset,
    radius: Float,
    open: Float,
    gray: Float,
    alpha: Float,
) {
    if (open <= 0f || alpha <= 0f) return

    val discRadius = radius * open
    val core = lerp(start = necroBladeCore, stop = portalGrayCore, fraction = gray)
    val mid = lerp(start = necroGlowGreen, stop = portalGrayCore, fraction = gray)
    val rim = lerp(start = necroSnathGreen, stop = portalGrayEdge, fraction = gray)

    scale(scaleX = 1f, scaleY = PORTAL_FLATTEN, pivot = center) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(core, mid, rim, Color.Transparent),
                    center = center,
                    radius = discRadius,
                ),
            radius = discRadius,
            center = center,
            alpha = alpha,
        )
        drawCircle(
            color = mid,
            radius = discRadius,
            center = center,
            style = Stroke(width = radius * 0.06f),
            alpha = alpha * 0.85f,
        )
    }
}

/** Draws the raised minion, dispatching on its [kind]; [MinionKind.None] draws nothing. */
internal fun DrawScope.drawMinion(
    kind: MinionKind,
    footCenter: Offset,
    height: Float,
    width: Float,
    alpha: Float,
) {
    when (kind) {
        MinionKind.None -> Unit

        MinionKind.Skeleton ->
            drawSkeleton(footCenter = footCenter, height = height, width = width, alpha = alpha)

        MinionKind.Golem ->
            drawGolem(footCenter = footCenter, height = height, width = width, alpha = alpha)
    }
}

/**
 * A white skeleton with green accents (SS-205): skull, spine, a couple of rib arcs, thin limb
 * bones, with the eye sockets and sternum lit by the Necromancer's spectral green.
 */
private fun DrawScope.drawSkeleton(footCenter: Offset, height: Float, width: Float, alpha: Float) {
    val centerX = footCenter.x
    val footY = footCenter.y
    val top = footY - height
    val halfWidth = width / 2f
    val boneStroke = height * 0.05f

    val skullRadius = height * 0.13f
    val skullCenter = Offset(x = centerX, y = top + skullRadius)
    val neckY = skullCenter.y + skullRadius
    val hipY = footY - height * 0.34f

    drawLine(
        color = boneWhite,
        start = Offset(x = centerX, y = neckY),
        end = Offset(x = centerX, y = hipY),
        strokeWidth = boneStroke * 1.2f,
        cap = StrokeCap.Round,
        alpha = alpha,
    )

    val ribCount = 3
    repeat(times = ribCount) { index ->
        val ribY = neckY + height * 0.05f + index * height * 0.06f
        val ribWidth = height * (0.17f - index * 0.02f)
        drawArc(
            color = boneShade,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(x = centerX - ribWidth, y = ribY - ribWidth * 0.4f),
            size = Size(width = ribWidth * 2f, height = ribWidth * 0.8f),
            style = Stroke(width = boneStroke * 0.7f),
            alpha = alpha,
        )
    }

    val shoulderY = neckY + height * 0.03f
    listOf(-1f, 1f).forEach { side ->
        drawLine(
            color = boneWhite,
            start = Offset(x = centerX, y = shoulderY),
            end = Offset(x = centerX + side * halfWidth, y = hipY - height * 0.02f),
            strokeWidth = boneStroke,
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        drawLine(
            color = boneWhite,
            start = Offset(x = centerX, y = hipY),
            end = Offset(x = centerX + side * halfWidth * 0.7f, y = footY),
            strokeWidth = boneStroke,
            cap = StrokeCap.Round,
            alpha = alpha,
        )
    }

    drawCircle(color = boneWhite, radius = skullRadius, center = skullCenter, alpha = alpha)
    val eyeRadius = skullRadius * 0.3f
    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = necroGlowGreen,
            radius = eyeRadius,
            center =
                Offset(
                    x = centerX + side * skullRadius * 0.42f,
                    y = skullCenter.y - skullRadius * 0.05f,
                ),
            alpha = alpha,
        )
    }
    drawCircle(
        color = necroGlowGreen,
        radius = boneStroke * 0.8f,
        center = Offset(x = centerX, y = neckY + height * 0.09f),
        alpha = alpha * 0.85f,
    )
}

/**
 * A hulking clay golem with green accents (SS-205): a boulder head over a heavy rounded torso with
 * thick limbs, brownish clay lit by glowing green runes and eyes so it reads as necromantic.
 */
private fun DrawScope.drawGolem(footCenter: Offset, height: Float, width: Float, alpha: Float) {
    val centerX = footCenter.x
    val footY = footCenter.y
    val top = footY - height
    val halfWidth = width / 2f

    val legWidth = width * 0.26f
    val legHeight = height * 0.22f
    val legTop = footY - legHeight
    listOf(-1f, 1f).forEach { side ->
        drawRoundRect(
            color = clayShade,
            topLeft = Offset(x = centerX + side * halfWidth * 0.5f - legWidth / 2f, y = legTop),
            size = Size(width = legWidth, height = legHeight),
            cornerRadius = CornerRadius(x = legWidth * 0.3f),
            alpha = alpha,
        )
    }

    val torsoTop = top + height * 0.3f
    val torsoBottom = legTop + legHeight * 0.3f
    val torsoRect = Offset(x = centerX - halfWidth, y = torsoTop)
    drawRoundRect(
        color = clayBody,
        topLeft = torsoRect,
        size = Size(width = width, height = torsoBottom - torsoTop),
        cornerRadius = CornerRadius(x = width * 0.24f),
        alpha = alpha,
    )
    drawRoundRect(
        color = clayLight,
        topLeft = Offset(x = centerX - halfWidth * 0.6f, y = torsoTop + height * 0.04f),
        size = Size(width = width * 0.3f, height = (torsoBottom - torsoTop) * 0.5f),
        cornerRadius = CornerRadius(x = width * 0.16f),
        alpha = alpha * 0.5f,
    )

    val armWidth = width * 0.22f
    listOf(-1f, 1f).forEach { side ->
        drawRoundRect(
            color = clayShade,
            topLeft = Offset(x = centerX + side * halfWidth - armWidth * 0.2f, y = torsoTop),
            size = Size(width = armWidth, height = (torsoBottom - torsoTop) * 0.9f),
            cornerRadius = CornerRadius(x = armWidth * 0.4f),
            alpha = alpha,
        )
    }

    val headRadius = height * 0.16f
    drawCircle(
        color = clayBody,
        radius = headRadius,
        center = Offset(x = centerX, y = top + headRadius),
        alpha = alpha,
    )

    val runeStroke = width * 0.05f
    listOf(-0.28f, 0.28f).forEach { offset ->
        drawLine(
            color = necroGlowGreen,
            start = Offset(x = centerX + width * offset, y = torsoTop + height * 0.08f),
            end = Offset(x = centerX + width * offset * 0.4f, y = torsoBottom - height * 0.05f),
            strokeWidth = runeStroke,
            cap = StrokeCap.Round,
            alpha = alpha * 0.9f,
        )
    }

    val eyeRadius = headRadius * 0.22f
    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = necroGlowGreen,
            radius = eyeRadius,
            center = Offset(x = centerX + side * headRadius * 0.4f, y = top + headRadius * 0.95f),
            alpha = alpha,
        )
    }
}
