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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.ArenaMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.getDisplayString
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_messages

internal val messagesIconSize = 56.dp
private val unreadCountFontSize = 20.sp
private val placeholderPanelGold = Color(color = 0xFFB8860B)
private const val UNREAD_DISPLAY_CAP = 9
private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val MESSAGES_EXPAND_LABEL = "MessagesExpand"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f
private val messageBarHeight = 64.dp

/**
 * The pinned top bar of the combat screen: a full-width golden bar with the messages icon and its
 * unread tally on the left, the most recent message beside it, and the [TurnTimer] on the right
 * (SS-173). Tapping the messages region toggles the message log via [onToggleExpanded]. The log
 * itself is drawn separately by [MessageLogOverlay] so it can paint over the combat content rather
 * than pushing it down.
 */
@Composable
internal fun MessagesBar(
    latestMessage: ArenaMessageState?,
    secondsRemaining: Int,
    isTimerVisible: Boolean,
    isTimerUrgent: Boolean,
    unreadMessageCount: Int,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = messageBarHeight)
                .background(color = placeholderPanelGold),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier.weight(weight = 1f)
                    .fillMaxHeight()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MessagesIcon(unreadMessageCount = unreadMessageCount)

            Text(
                text = latestMessage?.getDisplayString().orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f),
            )
        }

        TurnTimer(
            secondsRemaining = secondsRemaining,
            isVisible = isTimerVisible,
            isUrgent = isTimerUrgent,
        )
    }
}

/** The circular messages affordance with the flowing-color unread tally drawn in its center. */
@Composable
private fun MessagesIcon(unreadMessageCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size = messagesIconSize).clip(shape = CircleShape),
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
 * The message log, unfurled as an overlay over the combat content (SS-173). It drops down from the
 * [MessagesBar] and stops at the top of the dice-home slot in [diceHomeBounds], covering the
 * topmost avatar but leaving the dice home and buttons untouched. It paints over the avatars rather
 * than reserving layout space, so nothing shifts as it opens. The hoisted [isExpanded] drives the
 * unfurl height; it renders nothing while collapsed or before the dice-home slot has been measured.
 */
@Composable
internal fun MessageLogOverlay(
    messages: List<ArenaMessageState>,
    isExpanded: Boolean,
    diceHomeBounds: Rect?,
    modifier: Modifier = Modifier,
) {
    val expandedFraction by
        animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = MESSAGES_EXPAND_LABEL)
    if (expandedFraction <= 0f || diceHomeBounds == null) return

    val fullLogHeight =
        with(receiver = LocalDensity.current) { diceHomeBounds.top.toDp() } - messageBarHeight

    Box(modifier = modifier.fillMaxSize()) {
        ArenaMessageLog(
            messages = messages,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = messageBarHeight)
                    .height(height = fullLogHeight * expandedFraction)
                    .background(color = placeholderPanelGold),
        )
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
                fontSize = unreadCountFontSize,
                fontWeight = FontWeight.Bold,
            ),
        modifier = modifier,
    )
}
