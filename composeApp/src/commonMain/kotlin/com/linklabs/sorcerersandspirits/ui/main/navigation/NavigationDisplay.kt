package com.linklabs.sorcerersandspirits.ui.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.linklabs.sorcerersandspirits.ui.screens.credits.ui.CreditsScreen
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
    var isCapturingScreen by remember { mutableStateOf(value = false) }

    val captureScreenThen: (performPop: () -> Unit) -> Unit = { performPop ->
        scope.launch {
            isCapturingScreen = true
            withFrameNanos {}
            withFrameNanos {}
            dustSnapshot = screenLayer.toImageBitmap()
            isCapturingScreen = false
            performPop()
        }
    }

    val popWithDust: () -> Unit = { captureScreenThen { backStack.removeLastOrNull() } }
    val leaveArenaWithDust: () -> Unit = { captureScreenThen { backStack.popToTavern() } }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalNavigateBack provides popWithDust) {
            NavDisplay(
                backStack = backStack,
                entryProvider = { key ->
                    key.toScreen(
                        backStack = backStack,
                        popWithDust = popWithDust,
                        leaveArenaWithDust = leaveArenaWithDust,
                    )
                },
                onBack = { popWithDust() },
                entryDecorators = getRouteScopedEntryDecorators(),
                transitionSpec = { ForwardScreenTransition.entries.random().toContentTransform() },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                modifier =
                Modifier.fillMaxSize().drawWithContent {
                    val isCapturing = isCapturingScreen
                    if (isCapturing) {
                        screenLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer = screenLayer)
                    } else {
                        drawContent()
                    }
                },
            )
        }

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
