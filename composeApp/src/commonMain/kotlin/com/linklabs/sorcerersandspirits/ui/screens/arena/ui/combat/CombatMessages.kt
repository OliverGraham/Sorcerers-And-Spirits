package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.ArenaMessageLog
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_messages

internal val MESSAGES_ICON_SIZE = 56.dp
private val UNREAD_COUNT_FONT_SIZE = 20.sp
private val PLACEHOLDER_PANEL_GOLD = Color(color = 0xFFB8860B)
private const val UNREAD_DISPLAY_CAP = 9
private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val MESSAGES_EXPAND_LABEL = "MessagesExpand"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f

/**
 * The top-left messages affordance: a tapping target that expands the message log downward over the
 * head icons (stopping at the bottom half), with the unread count drawn in the center of the icon.
 */
@Composable
internal fun Messages(
    messages: List<ArenaMessageState>,
    unreadMessageCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedFraction by
        animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = MESSAGES_EXPAND_LABEL)

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight(fraction = 0.5f)) {
        if (expandedFraction > 0f) {
            ArenaMessageLog(
                messages = messages,
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(fraction = expandedFraction)
                        .background(color = PLACEHOLDER_PANEL_GOLD),
            )
        }

        MessagesIcon(
            unreadMessageCount = unreadMessageCount,
            onClick = onToggleExpanded,
            modifier = Modifier.align(alignment = Alignment.TopStart),
        )
    }
}

@Composable
private fun MessagesIcon(
    unreadMessageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size = MESSAGES_ICON_SIZE)
                .clip(shape = CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource = Res.drawable.ic_messages),
            contentDescription = "Messages",
            modifier = Modifier.fillMaxSize(),
        )

        if (unreadMessageCount > 0) {
            UnreadCount(count = unreadMessageCount)
        }
    }
}

/**
 * The unread count, drawn so the four class colors continuously flow through its digits — a looping
 * horizontal gradient whose tiled colors are shifted across the text forever. The tally reads "9+"
 * once ten or more messages go unread.
 */
@Composable
private fun UnreadCount(count: Int, modifier: Modifier = Modifier) {
    val colorTransition = rememberInfiniteTransition(label = UNREAD_COUNT_COLOR_TRANSITION_LABEL)
    val shift by
        colorTransition.animateFloat(
            initialValue = 0f,
            targetValue = UNREAD_COUNT_COLOR_TILE_WIDTH_PX,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = UNREAD_COUNT_COLOR_CYCLE_MILLIS,
                            easing = LinearEasing,
                        )
                ),
            label = UNREAD_COUNT_COLOR_SHIFT_LABEL,
        )

    val flowingColors = remember { classFamilyColors().let { it + it.first() } }
    val flowingBrush =
        Brush.linearGradient(
            colors = flowingColors,
            start = Offset(x = shift, y = 0f),
            end = Offset(x = shift + UNREAD_COUNT_COLOR_TILE_WIDTH_PX, y = 0f),
            tileMode = TileMode.Repeated,
        )

    val tally = if (count > UNREAD_DISPLAY_CAP) "$UNREAD_DISPLAY_CAP+" else count.toString()

    Text(
        text = tally,
        style =
            TextStyle(
                brush = flowingBrush,
                fontSize = UNREAD_COUNT_FONT_SIZE,
                fontWeight = FontWeight.Bold,
            ),
        modifier = modifier,
    )
}
