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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
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
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun NavigationHost(
    viewModel: AppViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
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
        NavHost(
            navController = navController,
            startDestination = TavernScreenRoute,
            modifier = modifier,
        ) {
            composable<TavernScreenRoute> {
                TavernScreen(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    displayName: String,
    onNameChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* navigate */ }
                .padding(vertical = 12.dp),
        )

        Text(
            text = "Screen B",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* navigate */ }
                .padding(vertical = 12.dp),
        )
    }
}
