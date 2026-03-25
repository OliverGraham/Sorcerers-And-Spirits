package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.RetryButton
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalTavernActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Serializable internal data object TavernScreenRoute

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinInject(),
    onNavigateToArena: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalTavernActions provides
            TavernActions(
                onActionButtonClick = viewModel::onActionButtonClick,
                onSendChallengerNotifications = viewModel::onSendChallengerNotifications,
                onAcceptArenaChallenge = viewModel::onAcceptArenaChallenge,
                onDeclineArenaChallenge = viewModel::onDeclineArenaChallenge,
                onShowArenaChallengeDialog = viewModel::showArenaChallengeDialog,
                onHideArenaChallengeDialog = viewModel::hideArenaChallengeDialog,
            )
    ) {
        viewModel.eventChannelFlow.collectEvents(
            onNavigateToArena = onNavigateToArena // TODO: make one for App?
        )
        val uiState by viewModel.uiStateFlow.collectAsState()

        TavernScreen(uiState = uiState, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernScreen(uiState: UiState<TavernState>, modifier: Modifier = Modifier) {
    when (uiState) {
        // TODO: Update UI to show error
        is UiState.Error -> RetryButton(retryMessage = "Retry", onRetry = {}, modifier = modifier)
        is UiState.Loaded -> LoadedTavernScreen(tavernState = uiState.data, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedTavernScreen(tavernState: TavernState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 140.dp,
        sheetContent = {
            TheBar(
                userTraveler = tavernState.userTraveler,
                otherTravelers = tavernState.otherTravelers,
            )
        },
    ) { padding ->
        ScaffoldContent(padding = padding)
    }

    if (tavernState.arenaChallenge != null && tavernState.showArenaChallengeDialog) {
        ArenaChallengeDialog(
            arenaChallenge = tavernState.arenaChallenge,
            userTraveler = tavernState.userTraveler,
        )
    }
}

@Composable
private fun ScaffoldContent(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues = padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO: What to put here?
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<TavernEvent>.collectEvents(onNavigateToArena: (String) -> Unit) {
    LaunchedEffect(Unit) {
        collect { event ->
            when (event) {
                is TavernEvent.NavigateToArena -> {
                    onNavigateToArena("")
                }
            }
        }
    }
}

// TODO: Preview
@Preview @Composable private fun TavernScreenPreview() {}
