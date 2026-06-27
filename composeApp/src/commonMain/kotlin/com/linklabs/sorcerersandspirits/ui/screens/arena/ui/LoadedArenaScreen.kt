package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalLatestRoll
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

private const val ARENA_STATE_TRANSITION_LABEL = "ArenaStateTransition"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoadedArenaScreen(
    screenState: ArenaScreenState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    CompositionLocalProvider(
        LocalLatestRoll provides screenState.latestRoll,
        LocalCharacterClassCopy provides screenState.classCopy,
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            ScreenContent(screenState = screenState, paddingValues = paddingValues)
        }

        if (screenState.showForfeitDialog) {
            ForfeitDialog()
        }
    }
}

@Composable
private fun ScreenContent(
    screenState: ArenaScreenState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // TODO: Remove this column and have each ArenaState composable invoke its own container
    Column(
        modifier = modifier.fillMaxSize().padding(paddingValues = paddingValues).padding(all = 4.dp)
    ) {
        val transition =
            updateTransition(
                targetState = screenState.arenaState,
                label = ARENA_STATE_TRANSITION_LABEL,
            )

        ArenaStateContent(screenState = screenState, transitionState = transition)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ArenaStateContent(
    screenState: ArenaScreenState,
    transitionState: Transition<ArenaState>,
    modifier: Modifier = Modifier,
) {
    // TODO: Make crossfade animation more dramatic
    transitionState.Crossfade(
        contentKey = { arenaState -> arenaState::class },
        modifier = modifier,
    ) { arenaState ->
        when (arenaState) {
            is ArenaState.ChoosingClasses ->
                arenaState.ArenaChoosingClasses(characterClasses = screenState.characterClasses)

            // SS-142: the combat screen is being rebuilt as NewArenaCombat; it is now live here
            // while logic is migrated over from the old ArenaCombat (kept until the move is
            // verified complete).
            is ArenaState.Combat ->
                NewArenaCombat(
                    state = arenaState,
                    messages = screenState.messages,
                    flashMessage = screenState.latestFlashMessage,
                )

            is ArenaState.WaitingForAllPlayers -> ArenaWaitingForPlayers()

            is ArenaState.Finished ->
                MatchFinishedContent(
                    finishedState = arenaState,
                    gauntletProgress = screenState.gauntletProgress,
                    hasReadiedForNextMatch = screenState.hasReadiedForNextMatch,
                    secondsRemaining = screenState.secondsRemaining,
                )
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
