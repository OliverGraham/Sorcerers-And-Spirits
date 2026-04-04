package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_swords
import sorcerersandspirits.composeapp.generated.resources.ic_up_arrow

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
                onSendChallengerNotifications = viewModel::onSendChallenge,
                onAcceptArenaChallenge = viewModel::onAcceptChallenge,
                onDeclineArenaChallenge = viewModel::onDeclineChallenge,
                onShowArenaChallengeDialog = viewModel::showChallengeDialog,
                onHideArenaChallengeDialog = viewModel::hideChallengeDialog,
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
    val arenaChallengeState = tavernState.challenge

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(all = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ChallengeDialogHandle(
                    showHandle = arenaChallengeState != null,
                    contentAlignment = Alignment.Start,
                )

                val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                AnimatedContent(
                    targetState =
                        if (isExpanded) Res.drawable.ic_martini else Res.drawable.ic_up_arrow,
                    label = "DragHandleIcon",
                ) { targetIcon ->
                    Icon(
                        painter = painterResource(resource = targetIcon),
                        contentDescription = null,
                        modifier = Modifier.size(size = 36.dp),
                    )
                }

                ChallengeDialogHandle(
                    showHandle = arenaChallengeState != null,
                    contentAlignment = Alignment.End,
                )
            }
        },
        sheetContent = {
            TheBar(
                userTraveler = tavernState.userTraveler,
                otherTravelers = tavernState.otherTravelers,
            )
        },
    ) { padding ->
        ScaffoldContent(padding = padding)
    }

    if (arenaChallengeState != null && arenaChallengeState.showChallengeDialog) {
        ArenaChallengeDialog(arenaChallenge = arenaChallengeState)
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
                is TavernEvent.NavigateToArena -> onNavigateToArena(event.arenaId)
                is TavernEvent.SendChallengeFailed -> println("TAVERN EVENT: SendChallengeFailed")
                is TavernEvent.NotEnoughChallengersAvailable ->
                    println("TAVERN EVENT: Show traveler unavailable")
            }
        }
    }
}

@Composable
private fun RowScope.ChallengeDialogHandle(
    showHandle: Boolean,
    contentAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = showHandle, modifier = modifier.weight(weight = 1f)) {
        IconButton(
            onClick = LocalTavernActions.current.onShowArenaChallengeDialog,
            modifier = Modifier.fillMaxWidth().wrapContentWidth(align = contentAlignment),
        ) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_swords),
                contentDescription = null,
                modifier = Modifier.size(size = 24.dp),
            )
        }
    }
}

// TODO: Preview
@Preview @Composable private fun TavernScreenPreview() {}
