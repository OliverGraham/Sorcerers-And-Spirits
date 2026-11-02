package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Incoming-challenge controls, provided by the navigation host so the challenge dialog (and the
 * Tavern's swords re-open handle) can drive accept/decline/show/hide from any screen.
 */
internal data class ChallengeActions(
    val onAcceptChallenge: (challengeId: String) -> Unit = {},
    val onDeclineChallenge: (challengeId: String) -> Unit = {},
    val onShowChallengeDialog: () -> Unit = {},
    val onHideChallengeDialog: () -> Unit = {},
    val onStartComputerOpponentMatch: () -> Unit = {},
)

internal val LocalChallengeActions = staticCompositionLocalOf { ChallengeActions() }

/**
 * Host-provided active challenge, so screens (e.g. the Tavern) can surface a re-open affordance.
 */
internal val LocalChallengeState = staticCompositionLocalOf<ChallengeState?> { null }

internal object ChallengeActionsProvider {
    val current: ChallengeActions
        @Composable @ReadOnlyComposable
        get() = LocalChallengeActions.current
}
