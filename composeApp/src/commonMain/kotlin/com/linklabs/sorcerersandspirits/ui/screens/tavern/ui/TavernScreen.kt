package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassPager
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.main.navigation.LocalOpenDrawer
import com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalTavernActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleTavernState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_menu
import sorcerersandspirits.composeapp.generated.resources.ic_swords
import sorcerersandspirits.composeapp.generated.resources.tavern_background

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinViewModel(),
    onNavigateToArena: (ArenaScreenViewModelArguments) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalTavernActions provides
            TavernActions(onSendChallengerNotifications = viewModel::onSendChallenge),
    ) {
        viewModel.eventChannelFlow.collectEvents(onNavigateToArena = onNavigateToArena)
        val uiState by viewModel.uiStateFlow.collectAsState()

        TavernScreen(uiState = uiState, onRetry = viewModel::retry, modifier = modifier)
    }
}

@Composable
private fun TavernScreen(
    uiState: UiState<TavernState>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)
        is UiState.Loaded -> LoadedTavernScreen(tavernState = uiState.data, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedTavernScreen(tavernState: TavernState, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier =
        modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(resource = Res.drawable.tavern_background),
                contentScale = ContentScale.Crop,
            ),
        containerColor = Color.Transparent,
        topBar = {
            TavernTopBar(onOpenBar = { scope.launch { scaffoldState.bottomSheetState.expand() } })
        },
        sheetPeekHeight = 0.dp,
        sheetDragHandle = null,
        sheetContainerColor = Color.Black.copy(alpha = 0.6f),
        sheetContentColor = Color.White,
        sheetTonalElevation = 0.dp,
        sheetContent = {
            TheBar(
                userTraveler = tavernState.userTraveler,
                otherTravelers = tavernState.otherTravelers,
            )
        },
    ) { padding ->
        CompositionLocalProvider(LocalCharacterClassCopy provides tavernState.classCopy) {
            ScreenContent(characterClasses = tavernState.characterClasses, padding = padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernTopBar(onOpenBar: () -> Unit, modifier: Modifier = Modifier) {
    val onOpenDrawer = LocalOpenDrawer.current
    val hasActiveChallenge = LocalChallengeState.current != null

    TopAppBar(
        title = {},
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onOpenBar) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_martini),
                    contentDescription = null,
                    modifier = Modifier.size(size = 48.dp),
                )
            }
        },
        actions = {
            ChallengeAction(showAction = hasActiveChallenge)
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_menu),
                    contentDescription = null,
                    modifier = Modifier.size(size = 48.dp),
                )
            }
        },
        expandedHeight = 48.dp,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ScreenContent(
    characterClasses: List<CharacterClassState>,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(paddingValues = padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CharacterClassPager(characterClasses = characterClasses)
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<TavernEvent>.collectEvents(
    onNavigateToArena: (ArenaScreenViewModelArguments) -> Unit,
) {
    LaunchedEffect(Unit) {
        collect { event ->
            when (event) {
                is TavernEvent.NavigateToArena -> onNavigateToArena(event.arguments)

                is TavernEvent.ShowChallengeSendFailed ->
                    println("TAVERN EVENT: ShowChallengeSendFailed")

                is TavernEvent.NotEnoughChallengersAvailable ->
                    println("TAVERN EVENT: Show traveler unavailable")
            }
        }
    }
}

@Composable
private fun ChallengeAction(showAction: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = showAction, modifier = modifier) {
        IconButton(onClick = LocalChallengeActions.current.onShowChallengeDialog) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_swords),
                contentDescription = null,
                modifier = Modifier.size(size = 24.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TavernScreenPreview() {
    SorcerersAndSpiritsTheme {
        TavernScreen(
            uiState = UiState.Loaded(data = exampleTavernState),
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
