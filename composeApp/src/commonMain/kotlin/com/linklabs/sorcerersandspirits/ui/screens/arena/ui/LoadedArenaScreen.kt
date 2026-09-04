package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.ArenaCombat
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

private const val ARENA_STATE_TRANSITION_LABEL = "ArenaStateTransition"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
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
            ScreenContent(screenState = screenState, timerState = timerState)
        }

        if (screenState.showForfeitDialog) {
            ForfeitDialog()
        }
    }
}

@Composable
private fun ScreenContent(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    modifier: Modifier = Modifier,
) {
    // TODO: Remove this column and have each ArenaState composable invoke its own container
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        val transition =
            updateTransition(
                targetState = screenState.arenaMatchState,
                label = ARENA_STATE_TRANSITION_LABEL,
            )

        ArenaStateContent(
            screenState = screenState,
            timerState = timerState,
            transitionState = transition,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ArenaStateContent(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    transitionState: Transition<ArenaMatchState>,
    modifier: Modifier = Modifier,
) {
    // TODO: Make crossfade animation more dramatic
    transitionState.Crossfade(
        contentKey = { arenaState -> arenaState::class },
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadedArenaScreenCollapsedPreview(
    @PreviewParameter(ArenaStatePreviewParameterProvider::class) screenState: ArenaScreenState,
) {
    SorcerersAndSpiritsTheme {
        LoadedArenaScreen(
            screenState = screenState,
            timerState = { ArenaTimerState(secondsRemaining = 0) },
        )
    }
}
