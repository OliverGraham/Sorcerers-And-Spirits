package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCards
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

//  TODO State is Choosing:
//      Pager of cards when choosing
//      Timer of 20 seconds
//      When choice is made, gets selected
//      When all selections made, go to next state,
//          transition of cards into Grid Arrangement (that are simplified and smaller but can be
//          expanded)
//          Arrangement moves in queue-like fashion, with current player's Card at top
//          then next up, etc.
//          Current player has fancy border and is large version and top-center
//          This arrangement is different depending on # of players
//              2 players: current on Top, other on Bottom (both large cards)
//              3+ players: Current on Top, Grid (smaller cards)

// TODO Arena: These should not be null if we're in the correct state/phase
@Composable
internal fun ArenaChoosingClasses(
    userPlayerState: PlayerState?,
    activePlayerState: PlayerState?,
    isItThisUsersTurn: Boolean,
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO Arena: This UI needs to change. It should always show the CharacterClassCards,
        // except
        //  that when another user selects a card, it becomes and stays selected for other users,
        //  and they cannot select it.
        //  When it's not the user's turn, they just observe
        if (isItThisUsersTurn) {
            UserChoosing(playerState = userPlayerState, characterClasses = characterClasses)
        } else {
            WaitingForOtherPlayer(activePlayerState = activePlayerState)
        }
    }
}

@Composable
private fun ColumnScope.UserChoosing(
    playerState: PlayerState?,
    characterClasses: List<CharacterClassState>,
) {
    var selectedClass by remember { mutableStateOf<CharacterClassState?>(null) }
    val onClassConfirmed = ArenaActionsProvider.current.onChooseCharacterClass

    Text(
        text = "Choose Your Class",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 24.dp),
    )

    CharacterClassCards(
        characterClasses = characterClasses,
        selectedClass = selectedClass,
        onClassSelected = { selectedClass = it },
        modifier = Modifier.weight(1f),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            val selected = selectedClass
            if (selected != null && playerState != null) {
                onClassConfirmed(playerState, selected)
                selectedClass = null
            }
        },
        enabled = selectedClass != null,
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        Text(text = "Confirm Selection")
    }
}

@Composable
private fun WaitingForOtherPlayer(activePlayerState: PlayerState?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Text(
            text =
                "${activePlayerState?.displayName ?: "Another player"} is choosing their class...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        // Add a loading indicator or animation here if desired
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaChoosingClassesPreview() {
    SorcerersAndSpiritsTheme {
        val mockPlayer =
            PlayerState(userId = "1", displayName = "Oliver", characterClassState = null)
        ArenaChoosingClasses(
            userPlayerState = mockPlayer,
            activePlayerState = mockPlayer,
            isItThisUsersTurn = true,
            characterClasses = CharacterClass.getAllClasses().map { it.toState() },
        )
    }
}
