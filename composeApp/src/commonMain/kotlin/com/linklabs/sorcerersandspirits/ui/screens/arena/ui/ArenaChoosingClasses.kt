package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCards
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun ArenaChoosingClasses(
    userPlayerState: PlayerState,
    activePlayerState: PlayerState,
    isItThisUsersTurn: Boolean,
    characterClasses: List<CharacterClassState>,
    allPlayerStates: List<PlayerState>,
    modifier: Modifier = Modifier,
) {
    // TODO Arena: these states should come from view model
    val unavailableClasses =
        remember(allPlayerStates, userPlayerState) {
            allPlayerStates
                .filter { it.userId != userPlayerState.userId }
                .mapNotNull {
                    when (it) {
                        is PlayerState.Selected -> it.characterClass
                        is PlayerState.Unselected -> null
                    }
                }
        }

    val classSelections =
        remember(allPlayerStates) {
            allPlayerStates.filterIsInstance<PlayerState.Selected>().associate { player ->
                player.characterClass to player.displayName
            }
        }

    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO Arena: resources, put back into separate functions
        Text(
            text =
                if (isItThisUsersTurn) {
                    "Choose Your Class"
                } else {
                    "${activePlayerState.displayName} is choosing their class..."
                },
            style =
                if (isItThisUsersTurn) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.headlineSmall
                },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp).padding(horizontal = 32.dp),
        )

        var selectedClass by remember { mutableStateOf<CharacterClassState?>(null) }
        val onClassConfirmed = ArenaActionsProvider.current.onChooseCharacterClass

        CharacterClassCards(
            characterClasses = characterClasses,
            selectedClass = selectedClass,
            unavailableClasses = unavailableClasses,
            classSelections = classSelections,
            onClassSelected = { if (isItThisUsersTurn) selectedClass = it },
            modifier = Modifier.weight(weight = 1f),
        )

        if (isItThisUsersTurn) {
            Spacer(modifier = Modifier.height(height = 24.dp))

            Button(
                onClick = {
                    val selected = selectedClass
                    if (selected != null) {
                        onClassConfirmed(userPlayerState, selected)
                        selectedClass = null
                    }
                },
                enabled = selectedClass != null,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(text = "Confirm Selection")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaChoosingClassesPreview() {
    SorcerersAndSpiritsTheme {
        val mockPlayer = PlayerState.Unselected(userId = "1", displayName = "")
        ArenaChoosingClasses(
            userPlayerState = mockPlayer,
            activePlayerState = mockPlayer,
            isItThisUsersTurn = true,
            characterClasses = CharacterClass.getAllClasses().map { it.toState() },
            allPlayerStates = listOf(mockPlayer),
        )
    }
}
