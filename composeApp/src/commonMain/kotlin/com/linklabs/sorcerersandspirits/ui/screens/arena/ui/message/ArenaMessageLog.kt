package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val OVERSCROLL_DISMISS_THRESHOLD_PX = 120f

@Composable
internal fun ArenaMessageLog(
    messages: List<ArenaMessageState>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val overscrollDismiss =
        rememberOverscrollDismissConnection(listState = listState, onDismiss = onDismiss)

    LazyColumn(
        state = listState,
        modifier = modifier.nestedScroll(connection = overscrollDismiss),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        items(items = messages.asReversed(), key = { it.id }) { messageState ->
            ArenaMessageItem(messageState = messageState, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun rememberOverscrollDismissConnection(
    listState: LazyListState,
    onDismiss: () -> Unit,
): NestedScrollConnection =
    remember(key1 = listState, key2 = onDismiss) {
        object : NestedScrollConnection {
            private var accumulatedOverscroll = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val isPullingPastEnd = available.y < 0f && !listState.canScrollForward
                if (isPullingPastEnd) {
                    accumulatedOverscroll += available.y
                    if (accumulatedOverscroll <= -OVERSCROLL_DISMISS_THRESHOLD_PX) {
                        accumulatedOverscroll = 0f
                        onDismiss()
                    }
                } else {
                    accumulatedOverscroll = 0f
                }
                return Offset.Zero
            }
        }
    }

@Composable
private fun ArenaMessageItem(messageState: ArenaMessageState, modifier: Modifier = Modifier) {
    when (messageState) {
        is ArenaMessageState.NewRound -> RoundMessage(state = messageState, modifier = modifier)

        is ArenaMessageState.ChoseCharacterClass ->
            ChoseCharacterClassMessage(state = messageState, modifier = modifier)

        is ArenaMessageState.ConnectedToArena ->
            ConnectedToArenaMessage(state = messageState, modifier = modifier)

        is ArenaMessageState.NewTurn -> NewTurnMessage(state = messageState, modifier = modifier)

        else -> DefaultMessage(state = messageState, modifier = modifier)
    }
}

@Composable
private fun DefaultMessage(state: ArenaMessageState, modifier: Modifier = Modifier) {
    Text(
        text = state.getDisplayString(),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun RoundMessage(state: ArenaMessageState.NewRound, modifier: Modifier = Modifier) {
    Text(
        text = state.getDisplayString(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 4.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                fullText = state.getDisplayString(),
                characterClassInfo = state.characterClassInfo,
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
        text = state.getDisplayString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(vertical = 1.dp),
    )
}

@Composable
private fun NewTurnMessage(state: ArenaMessageState.NewTurn, modifier: Modifier = Modifier) {
    Text(
        text =
            classHighlightedText(
                fullText = state.getDisplayString(),
                characterClassInfo = state.characterClassInfo,
            ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun classHighlightedText(
    fullText: String,
    characterClassInfo: CharacterClassMessageInfoState,
): AnnotatedString {
    val className = characterClassInfo.className
    val classColor = characterClassInfo.type.toColor()
    val classNameStart = fullText.lastIndexOf(string = className)

    return buildAnnotatedString {
        if (classNameStart < 0) {
            append(fullText)
            return@buildAnnotatedString
        }
        append(fullText.substring(startIndex = 0, endIndex = classNameStart))
        withStyle(style = SpanStyle(color = classColor)) { append(className) }
        append(fullText.substring(startIndex = classNameStart + className.length))
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogPreview() {
    SorcerersAndSpiritsTheme {
        val displayName = "A Mysterious Traveler"
        val rogueInfo = CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue)
        val paladinInfo = CharacterClassMessageInfoState("Paladin", CharacterClassType.Paladin)

        ArenaMessageLog(
            onDismiss = {},
            messages =
                listOf(
                    ArenaMessageState.ConnectedToArena(id = "1", displayName = displayName),
                    ArenaMessageState.ChoseCharacterClass(
                        id = "2",
                        displayName = displayName,
                        characterClassInfo = paladinInfo,
                    ),
                    ArenaMessageState.NewRound(id = "3", roundNumber = "1"),
                    ArenaMessageState.NewTurn(id = "4", characterClassInfo = rogueInfo),
                ),
        )
    }
}
