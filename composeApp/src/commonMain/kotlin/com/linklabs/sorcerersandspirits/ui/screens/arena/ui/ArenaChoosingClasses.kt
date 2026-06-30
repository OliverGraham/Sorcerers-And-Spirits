package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassWheel
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_choosing_classes_confirm
import sorcerersandspirits.composeapp.generated.resources.arena_choosing_classes_title
import sorcerersandspirits.composeapp.generated.resources.arena_choosing_classes_waiting

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
        ChoosingClassesTitle(
            isItThisUsersTurn = isItThisUsersTurn,
            activePlayerName = playerMap.getValue(key = activePlayerUserId).displayName,
        )

        ChoosingClassesCards(
            characterClasses = characterClasses,
            modifier = Modifier.weight(weight = 1f),
        )

        if (isItThisUsersTurn) {
            ConfirmSelectionButton()
        }
    }
}

@Composable
private fun ChoosingClassesTitle(isItThisUsersTurn: Boolean, activePlayerName: String) {
    Text(
        text =
            if (isItThisUsersTurn) {
                stringResource(resource = Res.string.arena_choosing_classes_title)
            } else {
                stringResource(
                    resource = Res.string.arena_choosing_classes_waiting,
                    activePlayerName,
                )
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
}

@Composable
private fun ArenaState.ChoosingClasses.ChoosingClassesCards(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
    val onClassSelected = ArenaActionsProvider.current.onSelectCharacterClass

    CharacterClassWheel(
        characterClasses = characterClasses,
        selectedClass = selectedClass,
        unavailableClasses = unavailableClasses,
        classSelections = classSelections,
        onClassSelected = { if (isItThisUsersTurn) onClassSelected(it) },
        modifier = modifier,
    )
}

@Composable
private fun ArenaState.ChoosingClasses.ConfirmSelectionButton() {
    val onClassConfirmed = ArenaActionsProvider.current.onChooseCharacterClass

    Spacer(modifier = Modifier.height(height = 24.dp))

    AppButton(
        onClick = {
            if (selectedClass != null) {
                onClassConfirmed(playerMap.getValue(key = userPlayerId), selectedClass)
            }
        },
        isEnabled = selectedClass != null,
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        Text(text = stringResource(resource = Res.string.arena_choosing_classes_confirm))
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
                unavailableClasses = emptySet(),
                classSelections = emptyMap(),
            )

        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            mockState.ArenaChoosingClasses(characterClasses = previewCharacterClassStates())
        }
    }
}
