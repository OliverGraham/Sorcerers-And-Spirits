package com.linklabs.ui.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.linklabs.dependencyinjection.commonModule
import com.linklabs.ui.navigation.NavigationHost
import com.linklabs.ui.theme.SorcerersAndSpiritsTheme
import org.koin.compose.KoinApplication

// TODO: OnLoad, show Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(application = { modules(modules = commonModule) }) {
        SorcerersAndSpiritsTheme {
            NavigationHost()
        }
    }
}
