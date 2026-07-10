package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin

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

/** The dark steel the shaded side of the maul head falls into, below [steelShade] (SS-199). */
internal val maceSteelDeep = Color(color = 0xFF6C7884)

/** The near-black iron the two-handed haft is forged from, so the bright head reads as the mass. */
private val haftIron = Color(color = 0xFF2C333B)
private val haftIronLit = Color(color = 0xFF515C68)

/** The ornate golds the maul is banded and bound with — a bright face and a deep base. */
internal val paladinGold = Color(color = 0xFFF3C94E)
internal val paladinGoldDeep = Color(color = 0xFFB0842A)

/** The green essence the swing trails, brightening to a hot core at the leading edge. */
internal val paladinEssenceCore = Color(color = 0xFFEBFFC6)
internal val paladinEssenceGreen = Color(color = 0xFF74E63A)
internal val paladinEssenceGlow = Color(color = 0xFF4FD836)

/** Half the width of the maul's blocky head across its striking faces. */
private val maceHeadHalfWidth = 23.dp

/** How far the head runs from its leading striking face back up toward the haft. */
private val maceHeadLength = 58.dp

/** How far the two-handed haft runs back from the head to the pommel. */
private val maceHaftLength = 128.dp

/**
 * The Paladin's massive two-handed maul in local coordinates (SS-199): a fearsome sledgehammer of a
 * weapon, not a wand. The blocky steel head — a heavy hexagonal block ringed with gold
 * reinforcement bands and rivets — sits at the origin, its leading striking face the point that
 * bites the target, and a long iron haft with a gold-bound grip runs back up the negative-y axis to
 * a gold pommel. A green essence aura gathers on the head, swelling with [essence]. [alpha] fades
 * the whole weapon in and out.
 */
internal fun DrawScope.drawMaceShape(essence: Float, alpha: Float) {
    drawHeadEssenceAura(essence = essence, alpha = alpha)
    drawHaft(alpha = alpha)
    drawMaulHead(alpha = alpha)
}

/** The green force gathered on the head, a broad halo swelling brighter as the roll climbs. */
private fun DrawScope.drawHeadEssenceAura(essence: Float, alpha: Float) {
    if (essence <= 0f) return
    val auraAlpha = essence * alpha
    val center = Offset(x = 0f, y = -maceHeadLength.toPx() * 0.42f)
    val radius = maceHeadLength.toPx() * 0.95f
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(paladinEssenceGreen, Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
        alpha = auraAlpha * 0.5f,
    )
}

/**
 * The two-handed haft: a thick iron rod with gold langets off the head, a bound grip, and a pommel.
 */
private fun DrawScope.drawHaft(alpha: Float) {
    val halfWidth = 5.5.dp.toPx()
    val haftTop = -maceHeadLength.toPx() * 0.5f
    val pommelY = haftTop - maceHaftLength.toPx()

    drawRoundRect(
        brush =
            Brush.horizontalGradient(
                colors = listOf(haftIron, haftIronLit, haftIron),
                startX = -halfWidth,
                endX = halfWidth,
            ),
        topLeft = Offset(x = -halfWidth, y = pommelY),
        size = Size(width = halfWidth * 2, height = haftTop - pommelY),
        cornerRadius = CornerRadius(x = halfWidth),
        alpha = alpha,
    )

    for (langetSide in listOf(-1f, 1f)) {
        drawLine(
            color = paladinGold,
            start = Offset(x = langetSide * halfWidth, y = haftTop),
            end = Offset(x = langetSide * halfWidth, y = haftTop - 30.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
    }

    val gripTop = haftTop - maceHaftLength.toPx() * 0.5f
    val gripBottom = haftTop - maceHaftLength.toPx() * 0.28f
    var wrapY = gripBottom
    while (wrapY >= gripTop) {
        drawLine(
            color = paladinGoldDeep,
            start = Offset(x = -halfWidth, y = wrapY),
            end = Offset(x = halfWidth, y = wrapY + 5.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        wrapY -= 9.dp.toPx()
    }

    drawCircle(
        color = paladinGold,
        radius = 7.dp.toPx(),
        center = Offset(x = 0f, y = pommelY),
        alpha = alpha,
    )
    drawCircle(
        color = paladinGoldDeep,
        radius = 7.dp.toPx(),
        center = Offset(x = 0f, y = pommelY),
        style = Stroke(width = 1.5.dp.toPx()),
        alpha = alpha,
    )
}

/**
 * The heavy hexagonal head: a shaded steel block, gold reinforcement bands, rivets, and a bright
 * face.
 */
private fun DrawScope.drawMaulHead(alpha: Float) {
    val halfWidth = maceHeadHalfWidth.toPx()
    val length = maceHeadLength.toPx()
    val faceHalfWidth = halfWidth * 0.82f
    val shoulderY = -length
    val shoulderHalfWidth = halfWidth * 0.72f

    val head =
        Path().apply {
            moveTo(x = -faceHalfWidth, y = 0f)
            lineTo(x = -halfWidth, y = -length * 0.2f)
            lineTo(x = -halfWidth, y = -length * 0.78f)
            lineTo(x = -shoulderHalfWidth, y = shoulderY)
            lineTo(x = shoulderHalfWidth, y = shoulderY)
            lineTo(x = halfWidth, y = -length * 0.78f)
            lineTo(x = halfWidth, y = -length * 0.2f)
            lineTo(x = faceHalfWidth, y = 0f)
            close()
        }
    drawPath(
        path = head,
        brush =
            Brush.horizontalGradient(
                colors = listOf(steelShade, steelBright, maceSteelDeep),
                startX = -halfWidth,
                endX = halfWidth,
            ),
        alpha = alpha,
    )

    for (bandY in listOf(-length * 0.16f, -length * 0.82f)) {
        drawRoundRect(
            color = paladinGold,
            topLeft = Offset(x = -halfWidth - 1.5.dp.toPx(), y = bandY - 3.dp.toPx()),
            size = Size(width = halfWidth * 2 + 3.dp.toPx(), height = 6.dp.toPx()),
            cornerRadius = CornerRadius(x = 2.dp.toPx()),
            alpha = alpha,
        )
        for (rivetSide in listOf(-1f, 1f)) {
            drawCircle(
                color = paladinGoldDeep,
                radius = 2.dp.toPx(),
                center = Offset(x = rivetSide * halfWidth * 0.7f, y = bandY),
                alpha = alpha,
            )
        }
    }

    drawLine(
        color = Color.White,
        start = Offset(x = -faceHalfWidth, y = 0f),
        end = Offset(x = faceHalfWidth, y = 0f),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.9f,
    )
}
