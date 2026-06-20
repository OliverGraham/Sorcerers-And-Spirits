package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgress
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.preview.ArenaStatePreviewParameterProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

private const val ARENA_STATE_TRANSITION_LABEL = "ArenaStateTransition"

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
                isTimerVisible = screenState.isTimerVisible,
                isTimerUrgent = screenState.isTimerUrgent,
                latestMessage = screenState.latestMessage,
                unhandledMessagesCount = screenState.unhandledMessagesCount,
            )
        },
        sheetContent = { ArenaMessageLog(messages = screenState.filteredMessages) },
    ) { paddingValues ->
        Box {
            ScreenContent(
                arenaState = screenState.arenaState,
                characterClasses = screenState.characterClasses,
                gauntletProgress = screenState.gauntletProgress,
                hasReadiedForNextMatch = screenState.hasReadiedForNextMatch,
                paddingValues = paddingValues,
            )

            // TODO(SS-091): TEMPORARY dice debug overlay. Remove this block (and
            //  TemporaryDiceDebugOverlay.kt) once combat is stable.
            val isInOrAfterCombat =
                screenState.arenaState is ArenaState.Combat ||
                    screenState.arenaState is ArenaState.Finished
            if (isInOrAfterCombat) {
                TemporaryDiceDebugOverlay(
                    messages = screenState.messages,
                    modifier =
                        Modifier.align(alignment = Alignment.TopCenter)
                            .padding(paddingValues = paddingValues)
                            .padding(all = 8.dp),
                )
            }
        }
    }

    if (screenState.showForfeitDialog) {
        ForfeitDialog()
    }

    if (screenState.isKnockedOutDecisionOffered) {
        KnockedOutDialog()
    }
}

@Composable
private fun ScreenContent(
    arenaState: ArenaState,
    characterClasses: List<CharacterClassState>,
    gauntletProgress: GauntletProgress,
    hasReadiedForNextMatch: Boolean,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(paddingValues = paddingValues)) {
        val transition =
            updateTransition(targetState = arenaState, label = ARENA_STATE_TRANSITION_LABEL)

        ArenaStateContent(
            transitionState = transition,
            characterClasses = characterClasses,
            gauntletProgress = gauntletProgress,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ArenaStateContent(
    transitionState: Transition<ArenaState>,
    characterClasses: List<CharacterClassState>,
    gauntletProgress: GauntletProgress,
    hasReadiedForNextMatch: Boolean,
    modifier: Modifier = Modifier,
) {
    transitionState.Crossfade(
        contentKey = { arenaState -> arenaState::class },
        modifier = modifier,
    ) { arenaState ->
        when (arenaState) {
            is ArenaState.ChoosingClasses -> {
                arenaState.ArenaChoosingClasses(characterClasses = characterClasses)
            }

            is ArenaState.Combat -> arenaState.ArenaCombat()

            is ArenaState.WaitingForAllPlayers -> ArenaWaitingForPlayers()

            is ArenaState.Finished ->
                MatchFinishedContent(
                    finishedState = arenaState,
                    gauntletProgress = gauntletProgress,
                    hasReadiedForNextMatch = hasReadiedForNextMatch,
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
