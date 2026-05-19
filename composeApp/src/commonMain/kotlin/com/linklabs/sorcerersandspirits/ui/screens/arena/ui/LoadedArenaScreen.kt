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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(
    screenState: ArenaScreenState,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = {
            ArenaMessageLogHandle(
                secondsRemaining = screenState.secondsRemaining,
                latestMessage = screenState.latestMessage,
                unhandledMessagesCount = screenState.unhandledMessagesCount,
            )
        },
        sheetContent = { ArenaMessageLog(messages = screenState.filteredMessages) },
    ) { paddingValues ->
        ScreenContent(arenaState = screenState.arenaState, paddingValues = paddingValues)
    }

    if (screenState.showForfeitDialog) {
        ForfeitDialog()
    }
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

                is ArenaState.Combat -> arenaState.ArenaCombat()

                is ArenaState.WaitingForAllPlayers -> ArenaWaitingForPlayers()

                // TODO Arena: Add some sort of message / UI / something for when the game ends
                is ArenaState.Finished -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadedArenaScreenCollapsedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) screenState: ArenaScreenState
) {
    SorcerersAndSpiritsTheme { LoadedArenaScreen(screenState = screenState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadedArenaScreenExpandedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) screenState: ArenaScreenState
) {
    SorcerersAndSpiritsTheme {
        val expandedBottomSheetState =
            rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)

        LoadedArenaScreen(
            screenState = screenState,
            scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = expandedBottomSheetState),
        )
    }
}
