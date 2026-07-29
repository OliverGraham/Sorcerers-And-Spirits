package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

private val portalGrayCore = Color(color = 0xFFBFC2BB)
private val portalGrayEdge = Color(color = 0xFF565A53)

/**
 * How flat the portal disc reads on the ground: a circle squashed to this fraction of its height.
 */
private const val PORTAL_FLATTEN = 0.42f

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

internal fun DrawScope.drawMinion(
    kind: MinionType,
    footCenter: Offset,
    height: Float,
    width: Float,
    alpha: Float,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val image =
        when (kind) {
            MinionType.None -> null
            MinionType.Skeleton -> skeletonImage
            MinionType.Golem -> golemImage
        } ?: return

    drawMinionImage(
        image = image,
        footCenter = footCenter,
        height = height,
        width = width,
        alpha = alpha,
    )
}

private fun DrawScope.drawMinionImage(
    image: ImageBitmap,
    footCenter: Offset,
    height: Float,
    width: Float,
    alpha: Float,
) {
    val imageWidth = image.width.toFloat()
    val imageHeight = image.height.toFloat()
    val scale = minOf(width / imageWidth, height / imageHeight)
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale

    drawImage(
        image = image,
        dstOffset =
            IntOffset(
                x = (footCenter.x - drawWidth / 2f).roundToInt(),
                y = (footCenter.y - drawHeight).roundToInt(),
            ),
        dstSize = IntSize(width = drawWidth.roundToInt(), height = drawHeight.roundToInt()),
        alpha = alpha,
    )
}
