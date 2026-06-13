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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun ArenaState.ChoosingClasses.ArenaChoosingClasses(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
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
                    "${playerMap.getValue(key = activePlayerUserId).displayName} is choosing their class..."
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

        val actions = ArenaActionsProvider.current
        val onClassConfirmed = actions.onChooseCharacterClass
        val onClassSelectedAction = actions.onSelectCharacterClass

        CharacterClassCards(
            characterClasses = characterClasses,
            selectedClass = selectedClass,
            unavailableClasses = unavailableClasses,
            classSelections = classSelections,
            onClassSelected = { if (isItThisUsersTurn) onClassSelectedAction(it) },
            modifier = Modifier.weight(weight = 1f),
        )

        if (isItThisUsersTurn) {
            Spacer(modifier = Modifier.height(height = 24.dp))

            Button(
                onClick = {
                    if (selectedClass != null) {
                        onClassConfirmed(playerMap.getValue(key = userPlayerId), selectedClass)
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
        val mockPlayer = PlayerState.Unselected(userId = "1", displayName = "Player")
        val mockState =
            ArenaState.ChoosingClasses(
                playerMap = emptyMap(),
                activePlayerUserId = mockPlayer.userId,
                userPlayerId = mockPlayer.userId,
                selectedClass = null,
                unavailableClasses = emptyList(),
                classSelections = emptyMap(),
            )

        mockState.ArenaChoosingClasses(
            characterClasses = CharacterClass.getAllClasses().map { it.toState() }
        )
    }
}
