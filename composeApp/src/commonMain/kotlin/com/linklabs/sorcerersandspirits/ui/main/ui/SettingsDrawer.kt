package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.linklabs.sorcerersandspirits.ui.main.bugreport.BugReportViewModel
import com.linklabs.sorcerersandspirits.ui.main.navigation.Route
import com.linklabs.sorcerersandspirits.ui.main.navigation.ScreenDissolveController
import com.linklabs.sorcerersandspirits.ui.main.navigation.popToNearestHomeScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.drawer_bug_report

@Composable
internal fun SettingsDrawer(
    backStack: NavBackStack<NavKey>,
    dissolveController: ScreenDissolveController,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    bugReportViewModel: BugReportViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()

    var isBugReportDialogVisible by remember { mutableStateOf(value = false) }
    val isBugReportingEnabled by bugReportViewModel.isBugReportingEnabledFlow.collectAsState()
    val bugReportSubmissionState by bugReportViewModel.submissionStateFlow.collectAsState()

    val isDrawerOpen = drawerState.isOpen
    LaunchedEffect(key1 = isDrawerOpen) {
        if (isDrawerOpen) {
            bugReportViewModel.refreshBugReportingEnabled()
        }
    }

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
        isBugReportingEnabled = isBugReportingEnabled,
        onReportBug = {
            bugReportViewModel.reset()
            isBugReportDialogVisible = true
        },
        modifier = modifier,
    )

    if (isBugReportDialogVisible) {
        BugReportDialog(
            submissionState = bugReportSubmissionState,
            onSubmit = bugReportViewModel::submit,
            onDismiss = {
                isBugReportDialogVisible = false
                bugReportViewModel.reset()
            },
        )
    }
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
    isBugReportingEnabled: Boolean,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().width(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        DrawerEntry(label = "Tavern", onClick = onNavigateToHome)
        DrawerEntry(label = "Profile", onClick = onNavigateToProfile)
        DrawerEntry(label = "Leaderboard", onClick = onNavigateToLeaderboard)
        DrawerEntry(label = "Achievements", onClick = onNavigateToAchievements)
        DrawerEntry(label = "Game Rules", onClick = onNavigateToGameRules)
        DrawerEntry(label = "Credits", onClick = onNavigateToCredits)

        if (isBugReportingEnabled) {
            DrawerEntry(
                label = stringResource(resource = Res.string.drawer_bug_report),
                onClick = onReportBug,
            )
        }
    }
}

@Composable
private fun DrawerEntry(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        DrawerEntryIconPlaceholder()
    }
}

@Composable
private fun DrawerEntryIconPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(size = 6.dp),
            ),
    )
}
