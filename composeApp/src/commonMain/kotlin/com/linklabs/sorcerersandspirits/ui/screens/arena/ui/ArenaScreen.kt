package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.RetryButton
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaEvent
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_up_arrow

@Composable
internal fun ArenaScreen(
    arguments: ArenaScreenViewModelArguments,
    viewModel: ArenaScreenViewModel = koinInject { parametersOf(arguments) },
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO Arena: CompositionLocalProvider

    viewModel.eventChannelFlow.collectEvents()
    val uiState by viewModel.uiStateFlow.collectAsState()

    ArenaScreen(uiState = uiState, modifier = modifier)
}

@Composable
private fun ArenaScreen(uiState: UiState<ArenaState>, modifier: Modifier = Modifier) {
    when (uiState) {
        // TODO Arena: Update UI to show error
        is UiState.Error -> RetryButton(retryMessage = "Retry", onRetry = {}, modifier = modifier)
        is UiState.Loaded -> LoadedArenaScreen(arenaState = uiState.data, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedArenaScreen(arenaState: ArenaState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = {
            SheetDragHandle(
                isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            )
        },
        sheetContent = {
            // TODO Arena: MessageLog
        },
    ) { paddingValues ->
        ScreenContent(arenaState = arenaState, paddingValues = paddingValues)
    }

    // TODO Arena: Dialogs go here
    // TODO Arena: Dialog showing that going back will forfeit the game
}

// TODO Arena: Make generic or just fix this one
@Composable
private fun SheetDragHandle(isExpanded: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = if (isExpanded) Res.drawable.ic_martini else Res.drawable.ic_up_arrow,
            label = "DragHandleIcon",
        ) { targetIcon ->
            Icon(
                painter = painterResource(resource = targetIcon),
                contentDescription = null,
                modifier = Modifier.size(size = 36.dp),
            )
        }
    }
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
                ArenaPhase.ChoosingClasses -> ArenaChoosingClasses()
                ArenaPhase.Combat -> ArenaCombat()
                ArenaPhase.WaitingForAllPlayers -> ArenaWaitingForPlayers()
                ArenaPhase.Finished -> {
                    // TODO Arena: figure out
                }
            }
        }
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<ArenaEvent>.collectEvents() {
    LaunchedEffect(Unit) { collect { event -> } }
}

// TODO Arena: Previews
