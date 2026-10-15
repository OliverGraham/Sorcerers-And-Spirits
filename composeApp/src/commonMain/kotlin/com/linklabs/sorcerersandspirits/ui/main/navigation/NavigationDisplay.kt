package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.linklabs.sorcerersandspirits.domain.audio.MusicPlayer
import com.linklabs.sorcerersandspirits.ui.main.navigation.transitions.ScreenDissolveEffect
import com.linklabs.sorcerersandspirits.ui.screens.achievements.ui.AchievementsScreen
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.ArenaScreen
import com.linklabs.sorcerersandspirits.ui.screens.credits.ui.CreditsScreen
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.GameRulesScreen
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui.LeaderboardScreen
import com.linklabs.sorcerersandspirits.ui.screens.profile.ui.ProfileScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreen
import org.koin.compose.koinInject

@Composable
internal fun NavigationDisplay(
    backStack: NavBackStack<NavKey>,
    dissolveController: ScreenDissolveController,
    modifier: Modifier = Modifier,
    musicPlayer: MusicPlayer = koinInject(),
) {
    val popBack: () -> Unit = {
        dissolveController.captureThenNavigate { backStack.removeLastOrNull() }
    }
    val leaveArena: () -> Unit = {
        dissolveController.captureThenNavigate { backStack.popToTavern() }
    }

    val currentRoute = backStack.lastOrNull() as? Route
    LaunchedEffect(key1 = currentRoute) {
        if (currentRoute != null) {
            musicPlayer.play(track = currentRoute.musicTrack())
        }
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalNavigateBack provides popBack) {
            NavDisplay(
                backStack = backStack,
                entryProvider = { key ->
                    key.toScreen(
                        backStack = backStack,
                        dissolveController = dissolveController,
                        popBack = popBack,
                        leaveArena = leaveArena,
                    )
                },
                onBack = { popBack() },
                entryDecorators = getRouteScopedEntryDecorators(),
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                modifier =
                Modifier.fillMaxSize().drawWithContent {
                    val isCapturing = dissolveController.isCapturingScreen
                    if (isCapturing) {
                        dissolveController.screenLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer = dissolveController.screenLayer)
                    } else {
                        drawContent()
                    }
                },
            )
        }

        val snapshot = dissolveController.snapshot
        if (snapshot != null) {
            ScreenDissolveEffect(
                snapshot = snapshot,
                onComplete = { dissolveController.onDissolveComplete() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun NavKey.toScreen(
    backStack: NavBackStack<NavKey>,
    dissolveController: ScreenDissolveController,
    popBack: () -> Unit,
    leaveArena: () -> Unit,
): NavEntry<NavKey> = when (this) {
    Route.TavernScreen -> NavEntry(key = this) { TavernScreen() }

    is Route.ArenaScreen ->
        NavEntry(key = this) {
            ArenaScreen(arguments = arguments.toDomain(), onBack = leaveArena)
        }

    Route.ProfileScreen ->
        NavEntry(key = this) {
            val signOut = LocalSignOut.current
            ProfileScreen(
                targetUserId = null,
                onProfileDeleted = signOut,
                onNavigateToAchievements = {
                    dissolveController.captureThenNavigate {
                        backStack.add(element = Route.AchievementsScreen)
                    }
                },
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
                    dissolveController.captureThenNavigate {
                        backStack.add(element = profileRoute)
                    }
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
