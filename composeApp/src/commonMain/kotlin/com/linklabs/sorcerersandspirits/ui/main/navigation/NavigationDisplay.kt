package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.linklabs.sorcerersandspirits.ui.main.navigation.transitions.DisintegrationEffect
import com.linklabs.sorcerersandspirits.ui.main.navigation.transitions.ForwardScreenTransition
import com.linklabs.sorcerersandspirits.ui.main.navigation.transitions.toContentTransform
import com.linklabs.sorcerersandspirits.ui.screens.achievements.ui.AchievementsScreen
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.ArenaScreen
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.GameRulesScreen
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui.LeaderboardScreen
import com.linklabs.sorcerersandspirits.ui.screens.profile.ui.ProfileScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreen
import kotlinx.coroutines.launch

@Composable
internal fun NavigationDisplay(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val screenLayer = rememberGraphicsLayer()
    var dustSnapshot by remember { mutableStateOf<ImageBitmap?>(value = null) }

    val popWithDust: () -> Unit = {
        scope.launch {
            dustSnapshot = screenLayer.toImageBitmap()
            backStack.removeLastOrNull()
        }
    }

    Box(modifier = modifier) {
        NavDisplay(
            backStack = backStack,
            entryProvider = { key ->
                key.toScreen(backStack = backStack, popWithDust = popWithDust)
            },
            onBack = { popWithDust() },
            entryDecorators = getRouteScopedEntryDecorators(),
            transitionSpec = { ForwardScreenTransition.entries.random().toContentTransform() },
            // The dust overlay covers the outgoing screen, so the system pop must stay silent to
            // avoid animating the same screen twice.
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    screenLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer = screenLayer)
                },
        )

        val snapshot = dustSnapshot
        if (snapshot != null) {
            DisintegrationEffect(
                snapshot = snapshot,
                onComplete = { dustSnapshot = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun NavKey.toScreen(
    backStack: NavBackStack<NavKey>,
    popWithDust: () -> Unit,
): NavEntry<NavKey> =
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
                ArenaScreen(arguments = arguments.toDomain(), onBack = popWithDust)
            }

        Route.ProfileScreen ->
            NavEntry(key = this) {
                ProfileScreen(
                    onProfileDeleted = popWithDust,
                    onNavigateToAchievements = { backStack.add(element = Route.AchievementsScreen) },
                )
            }

        Route.LeaderboardScreen -> NavEntry(key = this) { LeaderboardScreen() }

        Route.AchievementsScreen -> NavEntry(key = this) { AchievementsScreen() }

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
