package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(
    arenaState: ArenaState,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
) {
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
        Crossfade(targetState = arenaState::class) { _ ->
            when (arenaState) {
                is ArenaState.ChoosingClasses -> {
                    arenaState.ArenaChoosingClasses(
                        characterClasses = CharacterClass.getAllClasses().map { it.toState() }
                    )
                }

                is ArenaState.Combat -> ArenaCombat()

                is ArenaState.WaitingForAllPlayers -> ArenaWaitingForPlayers()

                is ArenaState.Finished -> {
                    // TODO Arena: figure out
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadedArenaScreenCollapsedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) arenaState: ArenaState
) {
    SorcerersAndSpiritsTheme { LoadedArenaScreen(arenaState = arenaState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadedArenaScreenExpandedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) arenaState: ArenaState
) {
    SorcerersAndSpiritsTheme {
        val expandedBottomSheetState =
            rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)

        LoadedArenaScreen(
            arenaState = arenaState,
            scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = expandedBottomSheetState),
        )
    }
}
