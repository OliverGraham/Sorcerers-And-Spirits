package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val messageBarHeight = 84.dp

private val messagesGradientTop = Color(color = 0xFF654E35)
private val messagesGradientMiddle = Color(color = 0xFFCB9C6A)
private val messagesGradientBottom = Color(color = 0xFF98754F)
private const val MESSAGES_GRADIENT_TOP_STOP = 0.0559f
private const val MESSAGES_GRADIENT_MIDDLE_STOP = 0.5176f
private const val MESSAGES_GRADIENT_BOTTOM_STOP = 0.9698f
private const val MESSAGES_BACKGROUND_ALPHA = 0.9f

internal fun messagesBackgroundBrush(
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush = Brush.verticalGradient(
    colorStops =
    arrayOf(
        MESSAGES_GRADIENT_TOP_STOP to
            messagesGradientTop.copy(alpha = MESSAGES_BACKGROUND_ALPHA),
        MESSAGES_GRADIENT_MIDDLE_STOP to
            messagesGradientMiddle.copy(alpha = MESSAGES_BACKGROUND_ALPHA),
        MESSAGES_GRADIENT_BOTTOM_STOP to
            messagesGradientBottom.copy(alpha = MESSAGES_BACKGROUND_ALPHA),
    ),
    startY = startY,
    endY = endY,
)
