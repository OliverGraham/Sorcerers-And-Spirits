package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult

@Immutable
data class AppState(
    val displayName: String,
    val showSignInDialog: Boolean = false,
    val isOffline: Boolean = false,
    val isNameTaken: Boolean = false,
    val linkResult: LinkAccountResult? = null,
)
