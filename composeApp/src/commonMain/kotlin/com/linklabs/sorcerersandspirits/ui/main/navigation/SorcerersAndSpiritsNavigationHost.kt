package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.OfflineBanner
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.main.ChallengeViewModel
import com.linklabs.sorcerersandspirits.ui.main.FirstScreenReadySignal
import com.linklabs.sorcerersandspirits.ui.main.splash.SplashGate
import com.linklabs.sorcerersandspirits.ui.main.ui.AppUpgradeDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.SettingsDrawer
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInNameDialog
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.ChallengeDialog
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.link_account_error_credential_in_use
import sorcerersandspirits.composeapp.generated.resources.link_account_error_generic
import sorcerersandspirits.composeapp.generated.resources.link_account_error_invalid_credential
import sorcerersandspirits.composeapp.generated.resources.link_account_error_unavailable

@Composable
internal fun SorcerersAndSpiritsNavigationHost(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsState()
    val appUpgradeStatus by viewModel.appUpgradeStatusFlow.collectAsState()
    val firstScreenReadySignal = koinInject<FirstScreenReadySignal>()
    val isFirstScreenReady by firstScreenReadySignal.isReady.collectAsState()

    val loadedState = uiState as? UiState.Loaded
    val isSignInRequired = loadedState?.data?.showSignInDialog == true
    val isAppReady = uiState !is UiState.Loading
    val isWaitingForFirstScreen = loadedState != null && !isSignInRequired && !isFirstScreenReady
    val canRevealContent = isAppReady && !isWaitingForFirstScreen

    SplashGate(canRevealContent = canRevealContent, modifier = modifier) { isContentRevealed ->
        when (val state = uiState) {
            is UiState.Error -> ErrorContent(onRetry = viewModel::retry, modifier = modifier)

            is UiState.Loaded -> {
                val appState = state.data
                val isOffline = appState.isOffline

                if (appState.showSignInDialog) {
                    if (isContentRevealed) {
                        SignInNameDialog(
                            displayName = appState.displayName,
                            isOffline = isOffline,
                            isNameTaken = appState.isNameTaken,
                            linkStatusMessage = linkErrorMessage(result = appState.linkResult),
                            onRandomNameClick = viewModel::getRandomName,
                            onAcceptClick = viewModel::createProfile,
                            onSocialMethodClick = { method ->
                                viewModel.onSignInWithSocialMethod(
                                    displayName = appState.displayName,
                                    method = method,
                                )
                            },
                            onEmailLink = { email, password ->
                                viewModel.onSignInWithEmail(
                                    displayName = appState.displayName,
                                    email = email,
                                    password = password,
                                )
                            },
                        )
                    }
                } else {
                    CompositionLocalProvider(LocalSignOut provides viewModel::onSignOut) {
                        SorcerersAndSpirits(isOffline = isOffline, modifier = modifier)
                    }
                }
            }

            is UiState.Loading -> LoadingIndicator(modifier = modifier)
        }
    }

    if (appUpgradeStatus != AppUpgradeStatus.UP_TO_DATE) {
        AppUpgradeDialog(status = appUpgradeStatus, onDismiss = viewModel::onDismissUpdateNudge)
    }
}

@Composable
private fun linkErrorMessage(result: LinkAccountResult?): String? = when (result) {
    LinkAccountResult.CREDENTIAL_IN_USE ->
        stringResource(resource = Res.string.link_account_error_credential_in_use)

    LinkAccountResult.INVALID_CREDENTIAL ->
        stringResource(resource = Res.string.link_account_error_invalid_credential)

    LinkAccountResult.NOT_AVAILABLE ->
        stringResource(resource = Res.string.link_account_error_unavailable)

    LinkAccountResult.NO_CURRENT_USER,
    LinkAccountResult.ERROR,
    -> stringResource(resource = Res.string.link_account_error_generic)

    LinkAccountResult.SUCCESS,
    LinkAccountResult.CANCELLED,
    null,
    -> null
}

@Composable
private fun SorcerersAndSpirits(
    isOffline: Boolean,
    challengeViewModel: ChallengeViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(navigationRoutesConfiguration, Route.TavernScreen)
    val dissolveController = rememberScreenDissolveController()

    LaunchedEffect(key1 = Unit) {
        challengeViewModel.navigateToArenaFlow.collect { arguments ->
            dissolveController.captureThenNavigate {
                backStack.add(element = Route.ArenaScreen(arguments = arguments.toSerializable()))
            }
        }
    }

    val challengeState by challengeViewModel.challengeStateFlow.collectAsState()

    CompositionLocalProvider(
        LocalChallengeActions provides
            ChallengeActions(
                onAcceptChallenge = challengeViewModel::onAcceptChallenge,
                onDeclineChallenge = challengeViewModel::onDeclineChallenge,
                onShowChallengeDialog = challengeViewModel::showChallengeDialog,
                onHideChallengeDialog = challengeViewModel::hideChallengeDialog,
            ),
        LocalChallengeState provides challengeState,
    ) {
        AppContent(
            isOffline = isOffline,
            backStack = backStack,
            dissolveController = dissolveController,
            modifier = modifier,
        )

        val challengeState = LocalChallengeState.current
        if (challengeState?.showChallengeDialog == true) {
            ChallengeDialog(challengeState = challengeState)
        }
    }
}

@Composable
private fun AppContent(
    isOffline: Boolean,
    backStack: NavBackStack<NavKey>,
    dissolveController: ScreenDissolveController,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DrawerDefaults.modalContainerColor.copy(alpha = 0.9f),
            ) {
                SettingsDrawer(
                    backStack = backStack,
                    dissolveController = dissolveController,
                    drawerState = drawerState,
                )
            }
        },
        gesturesEnabled = true,
    ) {
        Scaffold(modifier = modifier) { _ ->
            CompositionLocalProvider(
                LocalOpenDrawer provides { scope.launch { drawerState.open() } },
            ) {
                Column {
                    OfflineBanner(isOffline = isOffline)

                    NavigationDisplay(
                        backStack = backStack,
                        dissolveController = dissolveController,
                        modifier =
                        if (isOffline) {
                            modifier.consumeWindowInsets(insets = WindowInsets.statusBars)
                        } else {
                            modifier
                        },
                    )
                }
            }
        }
    }
}
