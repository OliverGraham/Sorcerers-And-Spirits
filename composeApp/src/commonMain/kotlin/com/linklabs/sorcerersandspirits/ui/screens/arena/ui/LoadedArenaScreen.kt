package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(arenaState: ArenaState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = {
            ArenaMessageLogHandle(
                secondsRemaining = arenaState.secondsRemaining,
                latestMessage = arenaState.latestMessage,
                unhandledMessagesCount = arenaState.unhandledMessagesCount,
            )
        },
        sheetContent = { ArenaMessageLog(messages = arenaState.filteredMessages) },
    ) { paddingValues ->
        ScreenContent(arenaState = arenaState, paddingValues = paddingValues)
    }

    // TODO Arena: Dialogs go here
    // TODO Arena: Dialog showing that going back will forfeit the game
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
