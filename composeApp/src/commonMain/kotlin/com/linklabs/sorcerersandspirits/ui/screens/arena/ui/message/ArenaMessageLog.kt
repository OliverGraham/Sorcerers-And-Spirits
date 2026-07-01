package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ArenaMessageLog(messages: List<ArenaMessageState>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        items(items = messages.asReversed(), key = { it.id }) { messageState ->
            ArenaMessageItem(messageState = messageState, modifier = Modifier.animateItem())
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
        modifier = modifier.padding(vertical = 4.dp),
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
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
}

@Composable
private fun ChoseCharacterClassMessage(
    state: ArenaMessageState.ChoseCharacterClass,
    modifier: Modifier = Modifier,
) {
    Text(
        text = state.getDisplayString(),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(vertical = 4.dp),
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
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun NewTurnMessage(state: ArenaMessageState.NewTurn, modifier: Modifier = Modifier) {
    Text(
        text = state.getDisplayString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogPreview() {
    SorcerersAndSpiritsTheme {
        val displayName = "A Mysterious Traveler"
        val rogueInfo = CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue)
        val paladinInfo = CharacterClassMessageInfoState("Paladin", CharacterClassType.Paladin)

        ArenaMessageLog(
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
                )
        )
    }
}
