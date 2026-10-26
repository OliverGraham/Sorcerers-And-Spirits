package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.main.navigation.transitions.DissolveStateContent
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.ArenaCombat
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

@Composable
internal fun LoadedArenaScreen(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    selectedOpponent: () -> String?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    CompositionLocalProvider(LocalCharacterClassCopy provides screenState.classCopy) {
        Scaffold(
            modifier =
            modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(resource = Res.drawable.arena_background),
                    contentScale = ContentScale.Crop,
                ),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { _ ->
            ArenaStateContent(
                screenState = screenState,
                timerState = timerState,
                selectedOpponent = selectedOpponent,
            )
        }

        if (screenState.showForfeitDialog) {
            ForfeitDialog()
        }
    }
}

@Composable
private fun ArenaStateContent(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    selectedOpponent: () -> String?,
    modifier: Modifier = Modifier,
) {
    DissolveStateContent(
        targetState = screenState.arenaMatchState,
        contentKey = { arenaState -> arenaState::class },
        isDramatic = { from, to -> !from.isWaitingScreen() && !to.isWaitingScreen() },
        modifier = modifier.fillMaxSize(),
    ) { arenaState ->
        when (arenaState) {
            is ArenaMatchState.ChoosingClasses ->
                arenaState.ArenaChoosingClasses(
                    screenState = screenState,
                    timerState = timerState,
                )

            is ArenaMatchState.Combat ->
                ArenaCombat(
                    screenState = screenState,
                    arenaMatchState = arenaState,
                    timerState = timerState,
                    selectedOpponent = selectedOpponent,
                )

            is ArenaMatchState.WaitingForAllPlayers -> ArenaWaitingForPlayers()

            is ArenaMatchState.Finished ->
                arenaState.MatchFinishedContent(
                    screenState = screenState,
                    timerState = timerState,
                )
        }
    }
}

private fun ArenaMatchState.isWaitingScreen(): Boolean = this is ArenaMatchState.WaitingForAllPlayers

@Preview
@Composable
private fun LoadedArenaScreenCollapsedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) screenState: ArenaScreenState,
) {
    SorcerersAndSpiritsTheme {
        LoadedArenaScreen(
            screenState = screenState,
            timerState = { ArenaTimerState(secondsRemaining = 0) },
            selectedOpponent = { null },
        )
    }
}
