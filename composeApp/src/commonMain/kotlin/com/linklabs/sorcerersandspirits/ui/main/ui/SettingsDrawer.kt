package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.linklabs.sorcerersandspirits.ui.main.navigation.Route
import com.linklabs.sorcerersandspirits.ui.main.navigation.ScreenDissolveController
import com.linklabs.sorcerersandspirits.ui.main.navigation.popToNearestHomeScreen
import kotlinx.coroutines.launch

@Composable
internal fun SettingsDrawer(
    backStack: NavBackStack<NavKey>,
    dissolveController: ScreenDissolveController,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    fun navigateTo(route: Route) {
        val isAlreadyOnRoute = backStack.lastOrNull() == route
        if (!isAlreadyOnRoute) {
            dissolveController.captureThenNavigate {
                backStack.popToNearestHomeScreen()
                backStack.add(route)
            }
        }
        scope.launch { drawerState.close() }
    }

    DrawerContent(
        onNavigateToHome = {
            val isAlreadyOnHome = backStack.lastOrNull() == Route.TavernScreen
            if (!isAlreadyOnHome) {
                dissolveController.captureThenNavigate {
                    backStack.clear()
                    backStack.add(element = Route.TavernScreen)
                }
            }
            scope.launch { drawerState.close() }
        },
        onNavigateToProfile = { navigateTo(route = Route.ProfileScreen) },
        onNavigateToLeaderboard = { navigateTo(route = Route.LeaderboardScreen) },
        onNavigateToAchievements = { navigateTo(route = Route.AchievementsScreen) },
        onNavigateToGameRules = { navigateTo(route = Route.GameRulesScreen) },
        onNavigateToCredits = { navigateTo(route = Route.CreditsScreen) },
        modifier = modifier,
    )
}

// TODO: StringResources

@Composable
private fun DrawerContent(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToGameRules: () -> Unit,
    onNavigateToCredits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().width(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        DrawerEntry(label = "Tavern", onClick = onNavigateToHome)
        DrawerEntry(label = "Profile", onClick = onNavigateToProfile)
        DrawerEntry(label = "Leaderboard", onClick = onNavigateToLeaderboard)
        DrawerEntry(label = "Achievements", onClick = onNavigateToAchievements)
        DrawerEntry(label = "Game Rules", onClick = onNavigateToGameRules)
        DrawerEntry(label = "Credits", onClick = onNavigateToCredits)
    }
}

@Composable
private fun DrawerEntry(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
