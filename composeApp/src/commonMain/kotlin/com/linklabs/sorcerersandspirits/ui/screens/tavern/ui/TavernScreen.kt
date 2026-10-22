package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassPager
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.LocalCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.main.FirstScreenReadySignal
import com.linklabs.sorcerersandspirits.ui.main.navigation.LocalOpenDrawer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.app_name
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_menu
import sorcerersandspirits.composeapp.generated.resources.ic_swords
import sorcerersandspirits.composeapp.generated.resources.retry_button
import sorcerersandspirits.composeapp.generated.resources.tavern_background
import sorcerersandspirits.composeapp.generated.resources.tavern_challenge_send_failed
import sorcerersandspirits.composeapp.generated.resources.tavern_challenge_travelers_unavailable

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalTavernActions provides
            TavernActions(onSendChallengerNotifications = viewModel::onSendChallenge),
    ) {
        val snackbarHostState = remember { SnackbarHostState() }

        viewModel.eventChannelFlow.collectEvents(
            snackbarHostState = snackbarHostState,
            onRetryChallenge = viewModel::retryLastChallenge,
        )
        val uiState by viewModel.uiStateFlow.collectAsState()

        val firstScreenReadySignal = koinInject<FirstScreenReadySignal>()
        val isTavernSettled = uiState !is UiState.Loading
        LaunchedEffect(key1 = isTavernSettled) {
            if (isTavernSettled) {
                firstScreenReadySignal.markReady()
            }
        }

        TavernScreen(
            uiState = uiState,
            onRetry = viewModel::retry,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
    }
}

@Composable
private fun TavernScreen(
    uiState: UiState<TavernState>,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)

        is UiState.Loaded ->
            LoadedTavernScreen(
                tavernState = uiState.data,
                snackbarHostState = snackbarHostState,
                modifier = modifier,
            )

        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

private val TavernTopBarHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedTavernScreen(
    tavernState: TavernState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier =
        modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(resource = Res.drawable.tavern_background),
                contentScale = ContentScale.Crop,
            ),
        containerColor = Color.Transparent,
        topBar = {
            val scope = rememberCoroutineScope()
            TavernTopBar(
                onToggleBar = {
                    scope.launch {
                        val bottomSheetState = scaffoldState.bottomSheetState
                        val isBarOpen = bottomSheetState.currentValue == SheetValue.Expanded
                        if (isBarOpen) {
                            bottomSheetState.partialExpand()
                        } else {
                            bottomSheetState.expand()
                        }
                    }
                },
            )
        },
        sheetContainerColor = Color.Black.copy(alpha = 0.8f),
        sheetContentColor = Color.White,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            val statusBarHeight =
                with(LocalDensity.current) {
                    WindowInsets.statusBars.getTop(density = this).toDp()
                }
            BoxWithConstraints {
                TheBar(
                    userTraveler = tavernState.userTraveler,
                    otherTravelers = tavernState.otherTravelers,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = maxHeight - statusBarHeight - TavernTopBarHeight),
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalCharacterClassCopy provides tavernState.classCopy) {
            ScreenContent(characterClasses = tavernState.characterClasses, padding = padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernTopBar(onToggleBar: () -> Unit, modifier: Modifier = Modifier) {
    val onOpenDrawer = LocalOpenDrawer.current
    val hasActiveChallenge = LocalChallengeState.current != null

    TopAppBar(
        title = {
            Text(
                text = stringResource(resource = Res.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onToggleBar) {
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
        expandedHeight = TavernTopBarHeight,
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
    snackbarHostState: SnackbarHostState,
    onRetryChallenge: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sendFailedMessage = stringResource(resource = Res.string.tavern_challenge_send_failed)
    val travelersUnavailableMessage =
        stringResource(resource = Res.string.tavern_challenge_travelers_unavailable)
    val retryLabel = stringResource(resource = Res.string.retry_button)

    LaunchedEffect(Unit) {
        collect { event ->
            when (event) {
                is TavernEvent.ShowChallengeSendFailed ->
                    scope.showRetryableChallengeError(
                        snackbarHostState = snackbarHostState,
                        message = sendFailedMessage,
                        retryLabel = retryLabel,
                        onRetryChallenge = onRetryChallenge,
                    )

                is TavernEvent.NotEnoughChallengersAvailable ->
                    scope.showRetryableChallengeError(
                        snackbarHostState = snackbarHostState,
                        message = travelersUnavailableMessage,
                        retryLabel = retryLabel,
                        onRetryChallenge = onRetryChallenge,
                    )
            }
        }
    }
}

private fun CoroutineScope.showRetryableChallengeError(
    snackbarHostState: SnackbarHostState,
    message: String,
    retryLabel: String,
    onRetryChallenge: () -> Unit,
) {
    launch {
        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = retryLabel,
                withDismissAction = true,
            )

        if (result == SnackbarResult.ActionPerformed) {
            onRetryChallenge()
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
            snackbarHostState = remember { SnackbarHostState() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
