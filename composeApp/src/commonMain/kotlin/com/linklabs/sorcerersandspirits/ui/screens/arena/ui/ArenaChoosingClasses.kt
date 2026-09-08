package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassWheel
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.ExpandingMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBarHeaderTarget
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.messageBarHeight
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_choosing_classes_confirm

private const val CONFIRM_BUTTON_VISIBILITY_LABEL = "ConfirmButtonVisibilityLabel"

@Composable
internal fun ArenaMatchState.ChoosingClasses.ArenaChoosingClasses(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    modifier: Modifier = Modifier,
) {
    var isLogExpanded by remember { mutableStateOf(value = false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MessagesBar(
                screenState = screenState,
                header =
                MessagesBarHeaderTarget.ChoosingClassesTurn(
                    activePlayerUserId = activePlayerUserId,
                    isThisUser = isItThisUsersTurn,
                    displayName = playerMap.getValue(key = activePlayerUserId).displayName,
                ),
                latestMessage = screenState.latestChoosingClassesMessage,
                timerState = timerState,
                isExpanded = isLogExpanded,
                onExpandedChange = { isLogExpanded = it },
                modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
            )

            CharacterClassWheel(
                characterClasses = screenState.characterClasses,
                modifier = Modifier.weight(weight = 1f),
            )

            ConfirmSelectionButton()
        }

        ExpandingMessageLog(
            isExpanded = isLogExpanded,
            messageGroups = screenState.messageGroups,
            onDismiss = { isLogExpanded = false },
            isCombatOngoing = false,
        )
    }
}

@Composable
private fun ArenaMatchState.ChoosingClasses.CharacterClassWheel(
    characterClasses: List<CharacterClassState>,
    modifier: Modifier = Modifier,
) {
    CharacterClassWheel(
        characterClasses = characterClasses,
        selectedClass = selectedClass,
        unavailableClasses = unavailableClasses,
        isItThisUsersTurn = isItThisUsersTurn,
        pinnedAuraClassType = userConfirmedClassType,
        modifier = modifier,
    )
}

@Composable
private fun ArenaMatchState.ChoosingClasses.ConfirmSelectionButton() {
    val onClassConfirmed = ArenaActionsProvider.current.onChooseCharacterClass
    val confirmableClass = selectedClass?.takeIf { it.type !in unavailableClasses }

    val hiddenFraction by animateFloatAsState(
        targetValue = if (isItThisUsersTurn) 0f else 1f,
        label = CONFIRM_BUTTON_VISIBILITY_LABEL,
    )

    Box(
        modifier = Modifier.clipToBounds(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ConfirmButton(
            isEnabled = isItThisUsersTurn && confirmableClass != null,
            onClick = {
                if (confirmableClass != null) {
                    onClassConfirmed(playerMap.getValue(key = userPlayerId), confirmableClass)
                }
            },
            modifier = Modifier.graphicsLayer {
                translationY = size.height * hiddenFraction
            },
        )
    }
}

@Composable
private fun ConfirmButton(isEnabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(
        onClick = onClick,
        isEnabled = isEnabled,
        modifier = modifier.padding(bottom = 16.dp),
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
            ArenaMatchState.ChoosingClasses(
                playerMap = mapOf(mockPlayer.userId to mockPlayer),
                activePlayerUserId = mockPlayer.userId,
                userPlayerId = mockPlayer.userId,
                selectedClass = null,
                unavailableClasses = emptySet(),
                classSelections = emptyMap(),
                userConfirmedClassType = null,
            )
        val mockScreenState =
            ArenaScreenState(
                showForfeitDialog = false,
                isUserPlayerKnockedOut = false,
                hasReadiedForNextMatch = false,
                arenaMatchState = mockState,
                messageGroups = emptyList(),
                characterClasses = previewCharacterClassStates(),
            )

        CompositionLocalProvider(LocalCharacterClassCopy provides previewCharacterClassCopy()) {
            mockState.ArenaChoosingClasses(
                screenState = mockScreenState,
                timerState = { ArenaTimerState(secondsRemaining = 0) },
            )
        }
    }
}
