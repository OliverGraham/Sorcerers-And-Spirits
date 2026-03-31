package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal data class TavernActions(
    val onActionButtonClick: () -> Unit = {},
    val onSendChallengerNotifications: (TravelerState, Set<TravelerState>) -> Unit = { _, _ -> },
    val onAcceptArenaChallenge: (ChallengeState) -> Unit = {},
    val onDeclineArenaChallenge: (ChallengeState) -> Unit = {},
    val onShowArenaChallengeDialog: () -> Unit = {},
    val onHideArenaChallengeDialog: () -> Unit = {},
)

internal val LocalTavernActions = staticCompositionLocalOf { TavernActions() }

internal object TavernActionsProvider {
    val current: TavernActions
        @Composable @ReadOnlyComposable get() = LocalTavernActions.current
}
