package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.ui.main.navigation.SorcerersAndSpiritsNavigationHost
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
fun App() {
    SorcerersAndSpiritsTheme { SorcerersAndSpiritsNavigationHost() }
}
