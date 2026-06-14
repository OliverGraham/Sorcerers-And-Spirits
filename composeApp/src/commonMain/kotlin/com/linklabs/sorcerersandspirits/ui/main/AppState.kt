package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.runtime.Immutable

@Immutable
data class AppState(
    val displayName: String,
    val showSignInDialog: Boolean = false,
    val isOffline: Boolean = false,
)
