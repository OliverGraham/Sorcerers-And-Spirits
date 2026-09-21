package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.linklabs.sorcerersandspirits.ui.screens.achievements.ui.AchievementsScreen
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.ArenaScreen
import com.linklabs.sorcerersandspirits.ui.screens.credits.ui.CreditsScreen
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.GameRulesScreen
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui.LeaderboardScreen
import com.linklabs.sorcerersandspirits.ui.screens.profile.ui.ProfileScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreen

@Composable
internal fun NavigationDisplay(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
    val popBack: () -> Unit = { backStack.removeLastOrNull() }
    val leaveArena: () -> Unit = { backStack.popToTavern() }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalNavigateBack provides popBack) {
            NavDisplay(
                backStack = backStack,
                entryProvider = { key ->
                    key.toScreen(
                        backStack = backStack,
                        popWithDust = popBack,
                        leaveArenaWithDust = leaveArena,
                    )
                },
                onBack = { popBack() },
                entryDecorators = getRouteScopedEntryDecorators(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun NavKey.toScreen(
    backStack: NavBackStack<NavKey>,
    popWithDust: () -> Unit,
    leaveArenaWithDust: () -> Unit,
): NavEntry<NavKey> = when (this) {
    Route.TavernScreen -> NavEntry(key = this) { TavernScreen() }

    is Route.ArenaScreen ->
        NavEntry(key = this) {
            ArenaScreen(arguments = arguments.toDomain(), onBack = leaveArenaWithDust)
        }

    Route.ProfileScreen ->
        NavEntry(key = this) {
            ProfileScreen(
                targetUserId = null,
                onProfileDeleted = popWithDust,
                onNavigateToAchievements = { backStack.add(element = Route.AchievementsScreen) },
            )
        }

    is Route.PlayerProfileScreen ->
        NavEntry(key = this) {
            ProfileScreen(
                targetUserId = userId,
                onProfileDeleted = {},
                onNavigateToAchievements = {},
            )
        }

    Route.LeaderboardScreen ->
        NavEntry(key = this) {
            LeaderboardScreen(
                onProfileClick = { userId, isCurrentUser ->
                    val profileRoute =
                        if (isCurrentUser) {
                            Route.ProfileScreen
                        } else {
                            Route.PlayerProfileScreen(userId = userId)
                        }
                    backStack.add(element = profileRoute)
                },
            )
        }

    Route.AchievementsScreen -> NavEntry(key = this) { AchievementsScreen() }

    Route.GameRulesScreen -> NavEntry(key = this) { GameRulesScreen() }

    Route.CreditsScreen -> NavEntry(key = this) { CreditsScreen() }

    else -> throw IllegalArgumentException("Unknown key: $this")
}

/**
 * View models and state-savers are scoped to their respective [Route]s. If popped off the back
 * stack, then these will be destroyed.
 */
@Composable
private fun <T : Any> getRouteScopedEntryDecorators(): List<NavEntryDecorator<T>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)
