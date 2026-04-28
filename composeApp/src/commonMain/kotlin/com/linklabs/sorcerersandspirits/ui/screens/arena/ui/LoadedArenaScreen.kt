package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.toDisplayText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(arenaState: ArenaState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = { ArenaMessageLogHandle(arenaState = arenaState) },
        sheetContent = { ArenaMessageLog(messages = arenaState.filteredMessages) },
    ) { paddingValues ->
        ScreenContent(arenaState = arenaState, paddingValues = paddingValues)
    }

    // TODO Arena: Dialogs go here
    // TODO Arena: Dialog showing that going back will forfeit the game
}

@Composable
private fun ArenaMessageLogHandle(arenaState: ArenaState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = arenaState.secondsRemaining,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(220, delayMillis = 90),
                ) togetherWith scaleOut(animationSpec = tween(220))
            },
        ) { seconds ->
            Text(text = "$seconds")
        }

        // TODO Arena: Improve animation
        AnimatedContent(
            targetState = arenaState.displayedMessage,
            modifier = Modifier.weight(weight = 1f),
            transitionSpec = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 1000, delayMillis = 90),
                ) togetherWith scaleOut(animationSpec = tween(durationMillis = 1000))
            },
        ) { displayedMessage ->
            if (displayedMessage != null) {
                Text(text = displayedMessage.toDisplayText())
            }

            if (arenaState.unhandledMessagesCount > 0) {
                val onIncomingMessageHandled = ArenaActionsProvider.current.onIncomingMessageHandled
                LaunchedEffect(key1 = transition.currentState, key2 = transition.targetState) {
                    if (transition.currentState == transition.targetState) {
                        delay(timeMillis = 1000)
                        onIncomingMessageHandled()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    arenaState: ArenaState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(paddingValues = paddingValues)) {
        Crossfade(targetState = arenaState.phase) { phase ->
            when (phase) {
                ArenaPhase.ChoosingClasses -> {
                    ArenaChoosingClasses(
                        userPlayerState = arenaState.userPlayer,
                        activePlayerState = arenaState.activePlayer,
                        isItThisUsersTurn = arenaState.isItThisUsersTurn,
                        characterClasses = CharacterClass.getAllClasses().map { it.toState() },
                        allPlayerStates = arenaState.playerStatesMap.values.toList(),
                    )
                }

                ArenaPhase.Combat -> ArenaCombat()

                ArenaPhase.WaitingForAllPlayers -> ArenaWaitingForPlayers()

                ArenaPhase.Finished -> {
                    // TODO Arena: figure out
                }
            }
        }
    }
}

// TODO Arena: Previews
