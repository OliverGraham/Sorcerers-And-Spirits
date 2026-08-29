package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.TurnTimer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_log_header
import sorcerersandspirits.composeapp.generated.resources.ic_messages

private val headerBookendReserve = 60.dp
private val messagesIconSize = 44.dp
private val unreadCountFontSize = 18.sp

private const val ROUND_HEADER_TRANSITION_LABEL = "MessagesRoundHeader"
private const val LATEST_MESSAGE_TRANSITION_LABEL = "MessagesLatestMessage"
private const val UNREAD_DISPLAY_CAP = 9
private const val UNREAD_COUNT_COLOR_TRANSITION_LABEL = "MessagesUnreadCountColor"
private const val UNREAD_COUNT_COLOR_SHIFT_LABEL = "MessagesUnreadCountColorShift"
private const val UNREAD_COUNT_CHANGE_LABEL = "MessagesUnreadCountChange"
private const val UNREAD_COUNT_COLOR_CYCLE_MILLIS = 2000
private const val UNREAD_COUNT_COLOR_TILE_WIDTH_PX = 80f

@Composable
internal fun MessagesBar(
    screenState: ArenaScreenState,
    roundNumber: Int,
    turnOwnerClassType: CharacterClassType,
    timerState: () -> ArenaTimerState,
    diceHomeBounds: Rect?,
    modifier: Modifier = Modifier,
) {
    val displayedMessageCount = screenState.displayedMessageCount
    var isExpanded by remember { mutableStateOf(value = false) }
    var seenLogMessageCount by remember { mutableStateOf(value = displayedMessageCount) }
    LaunchedEffect(key1 = isExpanded, key2 = displayedMessageCount) {
        val shouldSyncSeenCount =
            isExpanded || displayedMessageCount < seenLogMessageCount
        if (shouldSyncSeenCount) seenLogMessageCount = displayedMessageCount
    }
    val unreadMessageCount = (displayedMessageCount - seenLogMessageCount).coerceAtLeast(minimumValue = 0)

    var barBounds by remember { mutableStateOf<Rect?>(value = null) }

    MessagesBarSurface(
        roundNumber = roundNumber,
        turnOwnerClassType = turnOwnerClassType,
        latestMessage = screenState.latestCombatHeaderMessage,
        timerState = timerState,
        unreadMessageCount = unreadMessageCount,
        diceHomeBoundsTop = diceHomeBounds?.top,
        onToggleExpanded = {
            isExpanded = !isExpanded
            if (isExpanded) seenLogMessageCount = displayedMessageCount
        },
        modifier = modifier.onGloballyPositioned { barBounds = it.boundsInWindow() },
    )

    MessageLogDropdown(
        messageGroups = screenState.messageGroups,
        isExpanded = isExpanded,
        barBounds = barBounds,
        diceHomeBounds = diceHomeBounds,
        onDismiss = { isExpanded = false },
    )
}

@Composable
private fun MessagesBarSurface(
    roundNumber: Int,
    turnOwnerClassType: CharacterClassType,
    latestMessage: ArenaMessageState?,
    timerState: () -> ArenaTimerState,
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
            .padding(horizontal = 8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MessagesIcon(
                unreadMessageCount = unreadMessageCount,
                modifier = Modifier.align(alignment = Alignment.CenterStart),
            )

            AnimatedContent(
                targetState = roundNumber to turnOwnerClassType,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                },
                label = ROUND_HEADER_TRANSITION_LABEL,
                modifier =
                Modifier.align(alignment = Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = headerBookendReserve),
            ) { (round, classType) ->
                Text(
                    text = roundTurnHeaderText(roundNumber = round, classType = classType),
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
                timerState = timerState,
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

@Composable
private fun roundTurnHeaderText(roundNumber: Int, classType: CharacterClassType): AnnotatedString {
    val className = classType.name
    val fullText =
        stringResource(resource = Res.string.arena_log_header, roundNumber.toString(), className)
    val classNameStart = fullText.lastIndexOf(string = className)

    return buildAnnotatedString {
        if (classNameStart < 0) {
            append(text = fullText)
            return@buildAnnotatedString
        }

        append(text = fullText.substring(startIndex = 0, endIndex = classNameStart))
        withStyle(style = SpanStyle(color = classType.toColor())) { append(text = className) }
        append(text = fullText.substring(startIndex = classNameStart + className.length))
    }
}

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
                ),
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
