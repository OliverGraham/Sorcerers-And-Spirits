package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal data class TavernActions(
    val onSendChallengerNotifications: (TravelerState, Set<TravelerState>) -> Unit = { _, _ -> },
    val onAcceptChallenge: (challengeId: String) -> Unit = {},
    val onDeclineChallenge: (challengeId: String) -> Unit = {},
    val onShowChallengeDialog: () -> Unit = {},
    val onHideChallengeDialog: () -> Unit = {},
)

internal val LocalTavernActions = staticCompositionLocalOf { TavernActions() }

internal object TavernActionsProvider {
    val current: TavernActions
        @Composable @ReadOnlyComposable get() = LocalTavernActions.current
}
