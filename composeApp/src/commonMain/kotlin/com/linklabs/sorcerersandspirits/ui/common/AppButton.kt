package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val APP_BUTTON_LIFT = 6.dp
private val APP_BUTTON_FACE_COLOR = Color(color = 0xFF323232)
private const val APP_BUTTON_BASE_DARKEN = 0.5f
private const val APP_BUTTON_DISABLED_ALPHA = 0.4f
private const val APP_BUTTON_PRESS_LABEL = "AppButtonPress"

/**
 * The single 3D button style used across the whole app: a raised face over a darker base that drops
 * onto the base while pressed, so the button reads as physically pushed in. The [borderColor] rims
 * the face and tints the base depth; it defaults to the theme primary but can be handed a class
 * color where the screen is class-themed. Mirrors the arena combat buttons (SS-160) for one
 * consistent feel.
 */
@Composable
internal fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    isEnabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(size = 14.dp)
    val baseColor = lerp(start = borderColor, stop = Color.Black, fraction = APP_BUTTON_BASE_DARKEN)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressDrop by
        animateDpAsState(
            targetValue = if (isPressed) APP_BUTTON_LIFT else 0.dp,
            label = APP_BUTTON_PRESS_LABEL,
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier.alpha(alpha = if (isEnabled) 1f else APP_BUTTON_DISABLED_ALPHA)
                    .padding(bottom = APP_BUTTON_LIFT)
        ) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .offset(y = APP_BUTTON_LIFT)
                        .clip(shape = shape)
                        .background(color = baseColor)
            )

            Row(
                modifier =
                    Modifier.offset(y = pressDrop)
                        .clip(shape = shape)
                        .background(color = APP_BUTTON_FACE_COLOR)
                        .border(width = 2.dp, color = borderColor, shape = shape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = isEnabled,
                            onClick = onClick,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    ProvideTextStyle(value = MaterialTheme.typography.labelLarge) { content() }
                }
            }
        }
    }
}
