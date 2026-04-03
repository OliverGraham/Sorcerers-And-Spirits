package com.linklabs.sorcerersandspirits.ui.main.navigation

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
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.RetryButton
import com.linklabs.sorcerersandspirits.ui.main.AppState
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.main.ui.SettingsDrawer
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInNameDialog
import com.linklabs.sorcerersandspirits.ui.main.ui.TopBar
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun SorcerersAndSpiritsNavigationHost(
    viewModel: AppViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()

    when (val state = uiState) {
        // TODO: Update UI to show error
        is UiState.Error -> RetryButton(retryMessage = "Retry", onRetry = {}, modifier = modifier)
        is UiState.Loaded -> {
            val data = state.data

            if (data.showSignInDialog) {
                SignInNameDialog(
                    displayName = data.displayName,
                    onRandomNameClick = viewModel::getRandomName,
                    onAcceptClick = viewModel::saveNewTraveler,
                    onDismissRequest = {
                        /** TODO: what to do here, show another dialog? Snack bar? Just exit app? */
                    },
                )
            } else {
                SorcerersAndSpirits(
                    appState = data,
                    onUpdateDisplayName = viewModel::onUpdateDisplayName,
                    modifier = modifier,
                )
            }
        }
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@Composable
private fun SorcerersAndSpirits(
    appState: AppState,
    onUpdateDisplayName: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(navigationRoutesConfiguration, Route.TavernScreen)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SettingsDrawer(
                    backStack = backStack,
                    drawerState = drawerState,
                    scope = scope,
                    displayName = appState.displayName,
                    onUpdateDisplayName = onUpdateDisplayName,
                )
            }
        },
        gesturesEnabled = true,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = { TopBar(onMenuClick = { scope.launch { drawerState.open() } }) },
        ) { innerPadding ->
            NavigationDisplay(
                backStack = backStack,
                modifier = modifier.padding(paddingValues = innerPadding),
            )
        }
    }
}
