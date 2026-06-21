package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.navigation3.runtime.rememberNavBackStack
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.OfflineBanner
import com.linklabs.sorcerersandspirits.ui.main.AppState
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.main.ChallengeViewModel
import com.linklabs.sorcerersandspirits.ui.main.ui.AppUpgradeDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.SettingsDrawer
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInNameDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.TopBar
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeActions
import com.linklabs.sorcerersandspirits.ui.screens.tavern.LocalChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.ChallengeDialog
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.link_account_error_credential_in_use
import sorcerersandspirits.composeapp.generated.resources.link_account_error_generic
import sorcerersandspirits.composeapp.generated.resources.link_account_error_invalid_credential
import sorcerersandspirits.composeapp.generated.resources.link_account_error_unavailable

@Composable
internal fun SorcerersAndSpiritsNavigationHost(
    viewModel: AppViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()
    val appUpgradeStatus by viewModel.appUpgradeStatusFlow.collectAsState()

    when (val state = uiState) {
        is UiState.Error -> ErrorContent(onRetry = viewModel::retry, modifier = modifier)

        is UiState.Loaded -> {
            val data = state.data

            if (data.showSignInDialog) {
                SignInNameDialog(
                    displayName = data.displayName,
                    isOffline = data.isOffline,
                    isNameTaken = data.isNameTaken,
                    linkStatusMessage = linkErrorMessage(result = data.linkResult),
                    onRandomNameClick = viewModel::getRandomName,
                    onAcceptClick = viewModel::createProfile,
                    onSocialMethodClick = { method ->
                        viewModel.onSignInWithSocialMethod(
                            displayName = data.displayName,
                            method = method,
                        )
                    },
                    onEmailLink = { email, password ->
                        viewModel.onSignInWithEmail(
                            displayName = data.displayName,
                            email = email,
                            password = password,
                        )
                    },
                    // A profile is required before the app is usable, so the sign-in dialog is
                    // intentionally non-dismissible: dismiss attempts keep it on screen.
                    onDismissRequest = {},
                )
            } else {
                SorcerersAndSpirits(appState = data, modifier = modifier)
            }
        }

        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }

    // Overlays every state (including loading) so an unsupported client is gated before it can
    // touch the shared game state.
    if (appUpgradeStatus != AppUpgradeStatus.UP_TO_DATE) {
        AppUpgradeDialog(status = appUpgradeStatus, onDismiss = viewModel::onDismissUpdateNudge)
    }
}

/**
 * SUCCESS and CANCELLED don't warrant an in-dialog message (success navigates away; cancel is the
 * player's own choice), so they map to no message.
 */
@Composable
private fun linkErrorMessage(result: LinkAccountResult?): String? =
    when (result) {
        LinkAccountResult.CREDENTIAL_IN_USE ->
            stringResource(resource = Res.string.link_account_error_credential_in_use)

        LinkAccountResult.INVALID_CREDENTIAL ->
            stringResource(resource = Res.string.link_account_error_invalid_credential)

        LinkAccountResult.NOT_AVAILABLE ->
            stringResource(resource = Res.string.link_account_error_unavailable)

        LinkAccountResult.NO_CURRENT_USER,
        LinkAccountResult.ERROR -> stringResource(resource = Res.string.link_account_error_generic)

        LinkAccountResult.SUCCESS,
        LinkAccountResult.CANCELLED,
        null -> null
    }

@Composable
private fun SorcerersAndSpirits(
    appState: AppState,
    challengeViewModel: ChallengeViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(navigationRoutesConfiguration, Route.TavernScreen)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val challengeState by challengeViewModel.challengeStateFlow.collectAsState()

    // Hosted above the NavDisplay so an accepted challenge can route to the arena from any screen.
    LaunchedEffect(Unit) {
        challengeViewModel.navigateToArenaFlow.collect { arguments ->
            backStack.add(element = Route.ArenaScreen(arguments = arguments.toSerializable()))
        }
    }

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
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    SettingsDrawer(backStack = backStack, drawerState = drawerState, scope = scope)
                }
            },
            gesturesEnabled = true,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = { TopBar(onMenuClick = { scope.launch { drawerState.open() } }) },
            ) { innerPadding ->
                Column(modifier = Modifier.padding(paddingValues = innerPadding)) {
                    OfflineBanner(isOffline = appState.isOffline)

                    NavigationDisplay(backStack = backStack, modifier = modifier)
                }
            }
        }

        // A Dialog renders in its own window, so the challenge invite overlays whatever screen
        // (Tavern, Profile, Leaderboard, Game Rules) is currently showing.
        val activeChallenge = challengeState
        if (activeChallenge?.showChallengeDialog == true) {
            ChallengeDialog(challengeState = activeChallenge)
        }
    }
}
