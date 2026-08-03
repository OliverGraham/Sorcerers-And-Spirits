package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.ArenaMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message.getDisplayString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_log_header
import sorcerersandspirits.composeapp.generated.resources.ic_messages

internal val messagesIconSize = 44.dp
private val unreadCountFontSize = 18.sp
private val messagesGradientTop = Color(color = 0xFF654E35)
private val messagesGradientMiddle = Color(color = 0xFFCB9C6A)
private val messagesGradientBottom = Color(color = 0xFF98754F)
private const val MESSAGES_GRADIENT_TOP_STOP = 0.0559f
private const val MESSAGES_GRADIENT_MIDDLE_STOP = 0.5176f
private const val MESSAGES_GRADIENT_BOTTOM_STOP = 0.9698f
private const val MESSAGES_BACKGROUND_ALPHA = 0.9f
private const val UNREAD_DISPLAY_CAP = 9
private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val MESSAGES_EXPAND_LABEL = "MessagesExpand"
private const val ROUND_HEADER_TRANSITION_LABEL = "MessagesRoundHeader"
private const val LATEST_MESSAGE_TRANSITION_LABEL = "MessagesLatestMessage"
private const val UNREAD_COUNT_CHANGE_LABEL = "MessagesUnreadCountChange"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f
private val messageBarHeight = 84.dp
private val headerBookendReserve = 60.dp

@Composable
internal fun MessagesBar(
    roundNumber: Int,
    turnOwnerCharacterClass: CharacterClassState,
    latestMessage: ArenaMessageState?,
    secondsRemaining: Int,
    isTimerVisible: Boolean,
    isTimerUrgent: Boolean,
    unreadMessageCount: Int,
    diceHomeBoundsTop: Float?,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spanEndY =
        diceHomeBoundsTop ?: with(receiver = LocalDensity.current) { messageBarHeight.toPx() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = messageBarHeight)
                .background(brush = messagesBackgroundBrush(startY = 0f, endY = spanEndY))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MessagesIcon(
                unreadMessageCount = unreadMessageCount,
                modifier = Modifier.align(alignment = Alignment.CenterStart),
            )

            AnimatedContent(
                targetState = roundNumber to turnOwnerCharacterClass,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                },
                label = ROUND_HEADER_TRANSITION_LABEL,
                modifier =
                    Modifier.align(alignment = Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = headerBookendReserve),
            ) { (round, characterClass) ->
                Text(
                    text =
                        roundTurnHeaderText(roundNumber = round, characterClass = characterClass),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            TurnTimer(
                secondsRemaining = secondsRemaining,
                isVisible = isTimerVisible,
                isUrgent = isTimerUrgent,
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = latestMessage,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                },
                label = LATEST_MESSAGE_TRANSITION_LABEL,
            ) { message ->
                Text(
                    text = message?.getDisplayString().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The persistent handle header — "Round X · {class}'s turn" — with the class name tinted its family
 * color. Falls back to just the round when no turn owner is known (e.g. between matches).
 */
@Composable
private fun roundTurnHeaderText(
    roundNumber: Int,
    characterClass: CharacterClassState,
): AnnotatedString {
    val className = characterClass.className
    val fullText =
        stringResource(resource = Res.string.arena_log_header, roundNumber.toString(), className)
    val classNameStart = fullText.lastIndexOf(string = className)

    return buildAnnotatedString {
        if (classNameStart < 0) {
            append(text = fullText)
            return@buildAnnotatedString
        }

        append(text = fullText.substring(startIndex = 0, endIndex = classNameStart))
        withStyle(style = SpanStyle(color = characterClass.type.toColor())) {
            append(text = className)
        }
        append(text = fullText.substring(startIndex = classNameStart + className.length))
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
 * Tapping anywhere on the unfurled log dismisses it via [onDismiss].
 */
@Composable
internal fun MessageLogOverlay(
    messageGroups: List<ArenaMessageGroupState>,
    isExpanded: Boolean,
    diceHomeBounds: Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedFraction by
        animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = MESSAGES_EXPAND_LABEL)
    if (expandedFraction <= 0f || diceHomeBounds == null) return

    val barHeightPx = with(receiver = LocalDensity.current) { messageBarHeight.toPx() }
    val fullLogHeightPx = diceHomeBounds.top - barHeightPx
    val fullLogHeight = with(receiver = LocalDensity.current) { fullLogHeightPx.toDp() }

    Box(modifier = modifier.fillMaxSize()) {
        ArenaMessageLog(
            messageGroups = messageGroups,
            onDismiss = onDismiss,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = messageBarHeight)
                    .height(height = fullLogHeight * expandedFraction)
                    .background(
                        brush =
                            messagesBackgroundBrush(startY = -barHeightPx, endY = fullLogHeightPx)
                    )
                    .clickable(onClick = onDismiss),
        )
    }
}

/**
 * The messages surface's translucent wash (from Figma): a single top-to-bottom gradient — dark
 * brown at the top, through warm gold, to a muted tan at the bottom — dropped to
 * [MESSAGES_BACKGROUND_ALPHA] so combat still reads through it. [startY] and [endY] are pixel
 * offsets into the shared bar-top-to-dice-home span, so the collapsed [MessagesBar] and the
 * unfurled [MessageLogOverlay] sample one continuous gradient rather than each restarting its own.
 */
private fun messagesBackgroundBrush(startY: Float, endY: Float): Brush =
    Brush.verticalGradient(
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

    AnimatedContent(
        targetState = tally,
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn()) togetherWith
                (slideOutVertically { height -> -height } + fadeOut())
        },
        label = UNREAD_COUNT_CHANGE_LABEL,
        modifier = modifier,
    ) { value ->
        Text(
            text = value,
            style =
                TextStyle(
                    brush = flowingBrush,
                    fontSize = unreadCountFontSize,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}
