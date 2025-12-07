package com.linklabs.sorcerersandspirits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.linklabs.sorcerersandspirits.ui.main.AppViewModel
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreen
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernScreenRoute
import org.koin.compose.koinInject

@Composable
internal fun NavigationHost(
    viewModel: AppViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TavernScreenRoute,
        modifier = modifier,
    ) {
        composable<TavernScreenRoute> {
            TavernScreen()
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
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(height = 16.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { onNameChange(it) },
            label = { Text(text = "Display Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // Add other drawer items here
        Text(
            text = "Screen A",
            modifier = Modifier
                .clickable { /* navigate to ScreenA */ }
                .padding(8.dp),
        )

        Text(
            text = "Screen B",
            modifier = Modifier
                .clickable { /* navigate to ScreenB */ }
                .padding(8.dp),
        )
    }
}
