package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.ui.navigation.NavigationHost
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO: OnLoad, show Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    // KoinApplication(application = { modules(modules = commonModule) }) {
    SorcerersAndSpiritsTheme {
        NavigationHost()
    }
    // }
}
