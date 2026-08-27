package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaEvent
import com.linklabs.sorcerersandspirits.ui.screens.arena.LocalArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalHaptics
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.compositionlocals.LocalShakeDetector
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ArenaScreen(
    arguments: ArenaScreenViewModelArguments,
    viewModel: ArenaScreenViewModel = koinViewModel(parameters = { parametersOf(arguments) }),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalArenaActions provides
            ArenaActions(
                onSelectCharacterClass = viewModel::onSelectCharacterClass,
                onChooseCharacterClass = viewModel::onChooseCharacterClass,
                onAttack = viewModel::onAttack,
                onUseAbility = viewModel::onUseAbility,
                onDiceSettled = viewModel::onDiceSettled,
                onAnimationComplete = viewModel::onAnimationComplete,
                onTryLeaveArena = viewModel::onTryLeaveArena,
                onConfirmLeaveArena = viewModel::onConfirmLeaveArena,
                onDismissForfeitDialog = viewModel::onDismissForfeitDialog,
                onStayForAnotherMatch = viewModel::onStayForAnotherMatch,
                onTargetOpponent = viewModel::onTargetOpponent,
            ),
        LocalShakeDetector provides koinInject(),
        LocalHaptics provides koinInject(),
    ) {
        InterceptBackNavigationToShowForfeitDialog()

        val snackbarHostState = remember { SnackbarHostState() }
        viewModel.eventChannelFlow.collectEvents(
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
        val uiState by viewModel.uiStateFlow.collectAsState()
        val timerState = viewModel.timerStateFlow.collectAsState()

        ArenaScreen(
            uiState = uiState,
            timerState = { timerState.value },
            onRetry = viewModel::retry,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArenaScreen(
    uiState: UiState<ArenaScreenState>,
    timerState: () -> ArenaTimerState,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)

        is UiState.Loaded ->
            LoadedArenaScreen(
                screenState = uiState.data,
                timerState = timerState,
                snackbarHostState = snackbarHostState,
                modifier = modifier,
            )

        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<ArenaEvent>.collectEvents(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        collect { event ->
            when (event) {
                ArenaEvent.MatchTimedOut -> onBack()

                ArenaEvent.NavigateBack -> onBack()

                // Shown on its own scope so a lingering snackbar never stalls later events.
                is ArenaEvent.OpponentLeft ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "${event.displayName} left the arena"
                        )
                    }
            }
        }
    }
}

@Composable
private fun InterceptBackNavigationToShowForfeitDialog() {
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = true,
        onBackCompleted = ArenaActionsProvider.current.onTryLeaveArena,
    )
}
