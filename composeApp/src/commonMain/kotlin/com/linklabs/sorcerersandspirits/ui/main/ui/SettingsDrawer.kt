package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.linklabs.sorcerersandspirits.ui.main.bugreport.BugReportViewModel
import com.linklabs.sorcerersandspirits.ui.main.navigation.Route
import com.linklabs.sorcerersandspirits.ui.main.navigation.ScreenDissolveController
import com.linklabs.sorcerersandspirits.ui.main.navigation.popToNearestHomeScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.drawer_bug_report
import sorcerersandspirits.composeapp.generated.resources.ic_crown
import sorcerersandspirits.composeapp.generated.resources.ic_gavel
import sorcerersandspirits.composeapp.generated.resources.ic_martini
import sorcerersandspirits.composeapp.generated.resources.ic_menu_book
import sorcerersandspirits.composeapp.generated.resources.ic_person
import sorcerersandspirits.composeapp.generated.resources.ic_pest_control
import sorcerersandspirits.composeapp.generated.resources.ic_trophy

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
        currentRoute = backStack.lastOrNull(),
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
    currentRoute: NavKey?,
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
        DrawerEntry(
            label = "Tavern",
            icon = Res.drawable.ic_martini,
            isSelected = currentRoute == Route.TavernScreen,
            onClick = onNavigateToHome,
        )
        DrawerEntry(
            label = "Profile",
            icon = Res.drawable.ic_person,
            isSelected = currentRoute == Route.ProfileScreen,
            onClick = onNavigateToProfile,
        )
        DrawerEntry(
            label = "Leaderboard",
            icon = Res.drawable.ic_crown,
            isSelected = currentRoute == Route.LeaderboardScreen,
            onClick = onNavigateToLeaderboard,
        )
        DrawerEntry(
            label = "Achievements",
            icon = Res.drawable.ic_trophy,
            isSelected = currentRoute == Route.AchievementsScreen,
            onClick = onNavigateToAchievements,
        )
        DrawerEntry(
            label = "Game Rules",
            icon = Res.drawable.ic_gavel,
            isSelected = currentRoute == Route.GameRulesScreen,
            onClick = onNavigateToGameRules,
        )
        DrawerEntry(
            label = "Credits",
            icon = Res.drawable.ic_menu_book,
            isSelected = currentRoute == Route.CreditsScreen,
            onClick = onNavigateToCredits,
        )

        if (isBugReportingEnabled) {
            DrawerEntry(
                label = stringResource(resource = Res.string.drawer_bug_report),
                icon = Res.drawable.ic_pest_control,
                isSelected = false,
                onClick = onReportBug,
            )
        }
    }
}

@Composable
private fun DrawerEntry(
    label: String,
    icon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
        )
        Icon(
            painter = painterResource(resource = icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size = 32.dp),
        )
    }
}
