package com.linklabs.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.linklabs.ui.main.AppViewModel
import com.linklabs.ui.screens.tavern.TavernScreen
import com.linklabs.ui.screens.tavern.TavernScreenRoute
import org.koin.compose.koinInject

@Composable
internal fun NavigationHost(
    viewModel: AppViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TavernScreenRoute,
        modifier = modifier,
    ) {
        composable<TavernScreenRoute> {
            TavernScreen()
        }
    }
}
