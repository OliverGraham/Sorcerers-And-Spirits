package com.linklabs.sorcerersandspirits.ui.main

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult

@Immutable
data class AppState(
    val displayName: String,
    val showSignInDialog: Boolean = false,
    val isOffline: Boolean = false,
    val isNameTaken: Boolean = false,
    // Outcome of the most recent optional sign-in (account linking) attempt from the dialog.
    val linkResult: LinkAccountResult? = null,
)
