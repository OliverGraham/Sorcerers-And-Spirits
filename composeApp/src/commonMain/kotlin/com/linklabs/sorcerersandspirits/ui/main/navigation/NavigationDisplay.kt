package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.ArenaScreen
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.GameRulesScreen
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui.LeaderboardScreen
import com.linklabs.sorcerersandspirits.ui.screens.profile.ui.ProfileScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreen

@Composable
internal fun NavigationDisplay(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
    NavDisplay(
        backStack = backStack,
        entryProvider = { key -> key.toScreen(backStack = backStack) },
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = getRouteScopedEntryDecorators(),
        modifier = modifier,
    )
}

private fun NavKey.toScreen(backStack: NavBackStack<NavKey>): NavEntry<NavKey> =
    when (this) {
        Route.TavernScreen ->
            NavEntry(key = this) {
                TavernScreen(
                    onNavigateToArena = { arenaScreenViewModelArguments ->
                        backStack.add(
                            element =
                                Route.ArenaScreen(
                                    arguments = arenaScreenViewModelArguments.toSerializable()
                                )
                        )
                    }
                )
            }

        is Route.ArenaScreen ->
            NavEntry(key = this) {
                ArenaScreen(
                    arguments = arguments.toDomain(),
                    onBack = { backStack.removeLastOrNull() },
                )
            }

        Route.ProfileScreen ->
            NavEntry(key = this) {
                ProfileScreen(onProfileDeleted = { backStack.removeLastOrNull() })
            }

        Route.LeaderboardScreen -> NavEntry(key = this) { LeaderboardScreen() }

        Route.GameRulesScreen -> NavEntry(key = this) { GameRulesScreen() }

        else -> throw IllegalArgumentException("Unknown key: $this")
    }

/**
 * View models and state-savers are scoped to their respective [Route]s. If popped off the back
 * stack, then these will be destroyed.
 */
@Composable
private fun <T : Any> getRouteScopedEntryDecorators(): List<NavEntryDecorator<T>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )
