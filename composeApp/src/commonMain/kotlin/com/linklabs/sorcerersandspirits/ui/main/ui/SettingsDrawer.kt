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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.linklabs.sorcerersandspirits.ui.main.navigation.Route
import com.linklabs.sorcerersandspirits.ui.main.navigation.popToNearestHomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun SettingsDrawer(
    backStack: NavBackStack<NavKey>,
    drawerState: DrawerState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    fun navigateTo(route: Route) {
        backStack.popToNearestHomeScreen()
        backStack.add(route)
        scope.launch { drawerState.close() }
    }

    DrawerContent(
        onNavigateToHome = {
            backStack.clear()
            backStack.add(Route.TavernScreen)
            scope.launch { drawerState.close() }
        },
        onNavigateToProfile = { navigateTo(Route.ProfileScreen) },
        onNavigateToLeaderboard = { navigateTo(Route.LeaderboardScreen) },
        onNavigateToAchievements = { navigateTo(Route.AchievementsScreen) },
        onNavigateToGameRules = { navigateTo(Route.GameRulesScreen) },
        modifier = modifier,
    )
}

@Composable
private fun DrawerContent(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToGameRules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().width(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Menu", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        DrawerEntry(label = "Home", onClick = onNavigateToHome)
        DrawerEntry(label = "Profile", onClick = onNavigateToProfile)
        DrawerEntry(label = "Leaderboard", onClick = onNavigateToLeaderboard)
        DrawerEntry(label = "Achievements", onClick = onNavigateToAchievements)
        DrawerEntry(label = "Game Rules", onClick = onNavigateToGameRules)
    }
}

@Composable
private fun DrawerEntry(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
