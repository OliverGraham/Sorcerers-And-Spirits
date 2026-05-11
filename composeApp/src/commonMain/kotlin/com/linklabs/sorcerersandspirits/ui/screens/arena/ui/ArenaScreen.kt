package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.RetryButton
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaEvent
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.LocalArenaActions
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
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
                onIncomingMessageHandled = viewModel::onIncomingMessageHandled,
            )
    ) {
        viewModel.eventChannelFlow.collectEvents()
        val uiState by viewModel.uiStateFlow.collectAsState()

        ArenaScreen(uiState = uiState, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArenaScreen(uiState: UiState<ArenaState>, modifier: Modifier = Modifier) {
    when (uiState) {
        // TODO Arena: Update UI to show error
        is UiState.Error -> RetryButton(retryMessage = "Retry", onRetry = {}, modifier = modifier)

        is UiState.Loaded -> LoadedArenaScreen(arenaState = uiState.data, modifier = modifier)

        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<ArenaEvent>.collectEvents() {
    LaunchedEffect(Unit) { collect { event -> } }
}
