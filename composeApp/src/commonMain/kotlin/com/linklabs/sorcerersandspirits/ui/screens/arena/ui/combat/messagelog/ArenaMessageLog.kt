package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toLegibleOnDarkColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_log_finished
import sorcerersandspirits.composeapp.generated.resources.arena_log_round
import sorcerersandspirits.composeapp.generated.resources.arena_log_round_in_progress
import sorcerersandspirits.composeapp.generated.resources.arena_log_setup
import sorcerersandspirits.composeapp.generated.resources.arena_log_turn

private const val CARET_ROTATION_LABEL = "MessageLogCaretRotation"
private const val CARET_EXPANDED_ROTATION = 0f
private const val CARET_COLLAPSED_ROTATION = -90f

private const val SETUP_GROUP_KEY = 0
private const val FINISHED_GROUP_KEY = -1

private const val MESSAGE_BULLET_PREFIX = "•  "

private const val CLASS_START_TAG = "{class}"
private const val CLASS_END_TAG = "{/class}"

@Composable
internal fun ArenaMessageLog(
    messageGroups: List<ArenaMessageGroupState>,
    listState: LazyListState,
    dismissConnection: NestedScrollConnection,
    isCombatOngoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentRoundNumber =
        messageGroups.filterIsInstance<ArenaMessageGroupState.Round>().maxOfOrNull {
            it.roundNumber
        }
    val expandedGroups = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(key1 = messageGroups) { listState.animateScrollToItem(index = 0) }

    LazyColumn(
        state = listState,
        modifier = modifier.nestedScroll(connection = dismissConnection),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        for (group in messageGroups.asReversed()) {
            val groupKey = group.groupKey()
            val isCurrentRound =
                group is ArenaMessageGroupState.Round && group.roundNumber == currentRoundNumber
            val isDefaultExpanded = isCurrentRound || group is ArenaMessageGroupState.Finished
            val isExpanded = expandedGroups[groupKey] ?: isDefaultExpanded

            item(key = "group-$groupKey") {
                GroupHeader(
                    group = group,
                    isRoundInProgress = isCurrentRound && isCombatOngoing,
                    isExpanded = isExpanded,
                    onToggle = { expandedGroups[groupKey] = !isExpanded },
                    modifier = Modifier.animateItem(),
                )
            }

            if (!isExpanded) continue

            items(
                items =
                group.messages.filterNot {
                    it is ArenaMessageState.NewRound ||
                        it is ArenaMessageState.NormalRoll.Attack
                },
                key = { it.id },
            ) { message ->
                ArenaMessageItem(messageState = message, modifier = Modifier.animateItem())
            }
        }
    }
}

private fun ArenaMessageGroupState.groupKey(): Int = when (this) {
    is ArenaMessageGroupState.Setup -> SETUP_GROUP_KEY
    is ArenaMessageGroupState.Round -> roundNumber
    is ArenaMessageGroupState.Finished -> FINISHED_GROUP_KEY
}

@Composable
private fun GroupHeader(
    group: ArenaMessageGroupState,
    isRoundInProgress: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text =
        when (group) {
            is ArenaMessageGroupState.Setup -> stringResource(resource = Res.string.arena_log_setup)

            is ArenaMessageGroupState.Finished ->
                stringResource(resource = Res.string.arena_log_finished)

            is ArenaMessageGroupState.Round ->
                if (isRoundInProgress) {
                    stringResource(
                        resource = Res.string.arena_log_round_in_progress,
                        group.roundNumber.toString(),
                    )
                } else {
                    stringResource(
                        resource = Res.string.arena_log_round,
                        group.roundNumber.toString(),
                    )
                }
        }

    val caretRotation by
        animateFloatAsState(
            targetValue = if (isExpanded) CARET_EXPANDED_ROTATION else CARET_COLLAPSED_ROTATION,
            label = CARET_ROTATION_LABEL,
        )

    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Caret(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(degrees = caretRotation),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
internal fun Caret(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size = 10.dp)) { drawDownCaret(color = color) }
}

private fun DrawScope.drawDownCaret(color: Color) {
    val path =
        Path().apply {
            moveTo(0f, size.height * 0.25f)
            lineTo(size.width, size.height * 0.25f)
            lineTo(size.width / 2f, size.height * 0.75f)
            close()
        }
    drawPath(path = path, color = color)
}

@Composable
private fun ArenaMessageItem(messageState: ArenaMessageState, modifier: Modifier = Modifier) {
    when (messageState) {
        is ArenaMessageState.NewTurn -> TurnMessage(state = messageState, modifier = modifier)

        is ArenaMessageState.ChoseCharacterClass ->
            ChoseCharacterClassMessage(state = messageState, modifier = modifier)

        is ArenaMessageState.ConnectedToArena ->
            ConnectedToArenaMessage(state = messageState, modifier = modifier)

        else -> DefaultMessage(state = messageState, modifier = modifier)
    }
}

@Composable
private fun DefaultMessage(state: ArenaMessageState, modifier: Modifier = Modifier) {
    val displayText = state.getDisplayString()
    if (displayText.isBlank()) return
    Text(
        text =
        classHighlightedText(
            text = displayText,
            coloredClassNames = state.coloredClassNames,
            hasBullet = true,
        ),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun TurnMessage(state: ArenaMessageState.NewTurn, modifier: Modifier = Modifier) {
    Text(
        text =
        classHighlightedText(
            text =
            stringResource(
                resource = Res.string.arena_log_turn,
                state.characterClassInfo.className,
            ),
            coloredClassNames = state.coloredClassNames,
        ),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun ChoseCharacterClassMessage(
    state: ArenaMessageState.ChoseCharacterClass,
    modifier: Modifier = Modifier,
) {
    Text(
        text =
        classHighlightedText(
            text = state.getDisplayString(),
            coloredClassNames = state.coloredClassNames,
            hasBullet = true,
        ),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun ConnectedToArenaMessage(
    state: ArenaMessageState.ConnectedToArena,
    modifier: Modifier = Modifier,
) {
    Text(
        text =
        classHighlightedText(
            text = state.getDisplayString(),
            coloredClassNames = state.coloredClassNames,
            hasBullet = true,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(vertical = 1.dp),
    )
}

/**
 * Splitting on the [CLASS_START_TAG]/[CLASS_END_TAG] markers yields alternating segments: even
 * indices are plain text and odd indices are class names, so any number of tagged classes in one
 * string each get their own color.
 */
internal fun classHighlightedText(
    text: String,
    coloredClassNames: List<CharacterClassMessageInfoState>,
    hasBullet: Boolean = false,
): AnnotatedString {
    val colorByClassName =
        coloredClassNames.associate { it.className to it.type.toLegibleOnDarkColor() }
    val segments = text.split(CLASS_START_TAG, CLASS_END_TAG)

    return buildAnnotatedString {
        if (hasBullet) {
            append(MESSAGE_BULLET_PREFIX)
        }
        segments.forEachIndexed { index, segment ->
            val isClassNameSegment = index % 2 == 1
            val classColor = if (isClassNameSegment) colorByClassName[segment] else null
            if (classColor != null) {
                withStyle(style = SpanStyle(color = classColor)) { append(segment) }
            } else {
                append(segment)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogPreview() {
    SorcerersAndSpiritsTheme {
        val rogueInfo = CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue)
        val duelistInfo = CharacterClassMessageInfoState("Duelist", CharacterClassType.Duelist)

        ArenaMessageLog(
            listState = rememberLazyListState(),
            dismissConnection = remember { object : NestedScrollConnection {} },
            isCombatOngoing = true,
            messageGroups =
            listOf(
                ArenaMessageGroupState.Setup(
                    messages =
                    listOf(
                        ArenaMessageState.ConnectedToArena(
                            id = "1",
                            displayName = "A Traveler",
                        ),
                        ArenaMessageState.ChoseCharacterClass(
                            id = "2",
                            displayName = "A Traveler",
                            characterClassInfo = duelistInfo,
                        ),
                    ),
                ),
                ArenaMessageGroupState.Round(
                    roundNumber = 1,
                    messages =
                    listOf(
                        ArenaMessageState.NewRound(id = "3", roundNumber = 1),
                        ArenaMessageState.NewTurn(
                            id = "4",
                            characterClassInfo = duelistInfo,
                        ),
                        ArenaMessageState.KillingMomentum(
                            id = "5",
                            gestures = emptyMap(),
                            attackerUserId = "duelist",
                            animation = ArenaAnimationState.FlashAndAvatar(),
                        ),
                    ),
                ),
                ArenaMessageGroupState.Round(
                    roundNumber = 2,
                    messages =
                    listOf(
                        ArenaMessageState.NewRound(id = "6", roundNumber = 2),
                        ArenaMessageState.NewTurn(id = "7", characterClassInfo = rogueInfo),
                    ),
                ),
            ),
        )
    }
}
