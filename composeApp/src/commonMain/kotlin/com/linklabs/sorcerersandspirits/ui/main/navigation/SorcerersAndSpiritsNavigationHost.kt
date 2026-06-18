package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.OfflineBanner
import com.linklabs.sorcerersandspirits.ui.main.AppState
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.main.ui.AppUpgradeDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.SettingsDrawer
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInNameDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.TopBar
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

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
                    onRandomNameClick = viewModel::getRandomName,
                    onAcceptClick = viewModel::createProfile,
                    // A profile is required before the app is usable, so the sign-in dialog is
                    // intentionally non-dismissible: dismiss attempts keep it on screen.
                    onDismissRequest = {},
                )
            } else {
                SorcerersAndSpirits(
                    appState = data,
                    onUpdateDisplayName = viewModel::onUpdateDisplayName,
                    onDeleteProfile = viewModel::onDeleteProfile,
                    modifier = modifier,
                )
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

@Composable
private fun SorcerersAndSpirits(
    appState: AppState,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(navigationRoutesConfiguration, Route.TavernScreen)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

                NavigationDisplay(
                    backStack = backStack,
                    displayName = appState.displayName,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onDeleteProfile = onDeleteProfile,
                    modifier = modifier,
                )
            }
        }
    }
}
