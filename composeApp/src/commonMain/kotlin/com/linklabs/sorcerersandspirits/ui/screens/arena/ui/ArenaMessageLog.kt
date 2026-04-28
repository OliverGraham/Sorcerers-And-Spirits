package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RollTypeState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ArenaMessageLog(messages: List<ArenaMessageState>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        reverseLayout = true,
    ) {
        items(items = messages, key = { messageState -> messageState.id }) { messageState ->
            ArenaMessageItem(messageState = messageState, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun ArenaMessageItem(messageState: ArenaMessageState, modifier: Modifier = Modifier) {
    when (messageState) {
        is ArenaMessageState.Round -> RoundMessage(messageState, modifier)
        is ArenaMessageState.Action -> ActionMessage(messageState, modifier)
        is ArenaMessageState.Result -> ResultMessage(messageState, modifier)
        is ArenaMessageState.Generic -> GenericMessage(messageState, modifier)
    }
}

@Composable
private fun RoundMessage(state: ArenaMessageState.Round, modifier: Modifier = Modifier) {
    Text(
        text = "ROUND ${state.roundNumber}",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 4.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
}

@Composable
private fun ActionMessage(state: ArenaMessageState.Action, modifier: Modifier = Modifier) {
    Text(
        text =
            "${state.rollerName} rolled ${state.rollerValue} against ${state.defenderName}'s ${state.rollTypeState}",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ResultMessage(state: ArenaMessageState.Result, modifier: Modifier = Modifier) {
    Text(
        text =
            "${state.rollerName} is now rolling against ${state.defenderName}'s ${state.rollTypeState}!",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun GenericMessage(state: ArenaMessageState.Generic, modifier: Modifier = Modifier) {
    Text(
        text = state.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogPreview() {
    SorcerersAndSpiritsTheme {
        val displayName = "A Mysterious Traveler"
        val classNameAttacker = "Rogue"
        val classNameDefender = "Paladin"
        ArenaMessageLog(
            messages =
                listOf(
                    ArenaMessageState.Result(
                        id = "6",
                        rollerName = classNameAttacker,
                        defenderName = classNameDefender,
                        rollTypeState = RollTypeState.Health,
                    ),
                    ArenaMessageState.Action(
                        id = "5",
                        rollerName = classNameAttacker,
                        rollerValue = "12",
                        defenderName = classNameDefender,
                        rollTypeState = RollTypeState.ArmorClass,
                    ),
                    ArenaMessageState.Generic("4", "It is now $classNameAttacker's turn!"),
                    ArenaMessageState.Round("3", "1"),
                    ArenaMessageState.Generic("2", "Combat is now starting!"),
                    ArenaMessageState.Generic("1", "$displayName selected $classNameAttacker"),
                )
        )
    }
}
