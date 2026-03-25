package com.linklabs.sorcerersandspirits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.TavernScreenRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavigationHost(viewModel: AppViewModel = koinInject(), modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    displayName = "User Name", // Ideally get this from viewModel
                    onNameChange = { /* TODO: update viewModel */ },
                )
            }
        },
        gesturesEnabled = true,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = { TopBar(onMenuClick = { scope.launch { drawerState.open() } }) },
        ) {
            NavHost(
                navController = navController,
                startDestination = TavernScreenRoute,
                modifier = modifier,
            ) {
                composable<TavernScreenRoute> {
                    TavernScreen(
                        onNavigateToArena = {
                            // TODO: Change obviously
                            scope.launch { drawerState.open() }
                            // navController.navigate(route = "arena")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(text = "Sorcerers & Spirits", style = MaterialTheme.typography.headlineSmall)
        },
        actions = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_menu),
                    contentDescription = "Open Drawer",
                )
            }
        },
    )
}

@Composable
private fun DrawerContent(displayName: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().width(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(height = 16.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text(text = "Display Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Screen A",
            modifier =
                Modifier.fillMaxWidth().clickable { /* navigate */ }.padding(vertical = 12.dp),
        )

        Text(
            text = "Screen B",
            modifier =
                Modifier.fillMaxWidth().clickable { /* navigate */ }.padding(vertical = 12.dp),
        )
    }
}
