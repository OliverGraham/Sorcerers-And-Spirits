package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.linklabs.sorcerersandspirits.ui.main.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO: What to do with this?

@Composable
internal fun SettingsDrawer(
    backStack: NavBackStack<NavKey>,
    drawerState: DrawerState,
    scope: CoroutineScope,
    displayName: String,
    onUpdateDisplayName: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DrawerContent(
        displayName = displayName,
        onUpdateDisplayName = onUpdateDisplayName,
        onNavigateToTavern = {
            backStack.clear()
            backStack.add(Route.TavernScreen)
            scope.launch { drawerState.close() }
        },
        onNavigateToArena = {
            // backStack.add(Route.ArenaScreen(arenaId = "default"))
            scope.launch { drawerState.close() }
        },
        modifier = modifier,
    )
}

@Composable
private fun DrawerContent(
    displayName: String,
    onUpdateDisplayName: (String) -> Unit,
    onNavigateToTavern: () -> Unit,
    onNavigateToArena: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDisplayName by rememberSaveable { mutableStateOf(displayName) }

    // Keep local field in sync if displayName changes from outside.
    LaunchedEffect(displayName) { pendingDisplayName = displayName }

    Column(
        modifier = modifier.fillMaxHeight().width(300.dp).padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pendingDisplayName,
            onValueChange = { pendingDisplayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = { onUpdateDisplayName(pendingDisplayName) },
                enabled = pendingDisplayName != displayName,
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { pendingDisplayName = displayName },
                enabled = pendingDisplayName != displayName,
            ) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tavern",
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onNavigateToTavern)
                    .padding(vertical = 12.dp),
        )

        Text(
            text = "Arena",
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onNavigateToArena)
                    .padding(vertical = 12.dp),
        )
    }
}
