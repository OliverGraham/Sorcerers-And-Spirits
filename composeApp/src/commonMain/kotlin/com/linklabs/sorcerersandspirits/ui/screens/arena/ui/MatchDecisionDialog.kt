package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgress
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.MatchDecision
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun MatchDecisionDialog(decision: MatchDecision, gauntletProgress: GauntletProgress) {
    val actions = ArenaActionsProvider.current

    // A finished match only offers leaving once the whole Gauntlet is done; until then players just
    // continue to the next match (they can still forfeit via the back button).
    val isMatchFinishedMidGauntlet =
        decision == MatchDecision.MatchFinished && !gauntletProgress.isFinalMatch

    val title =
        when {
            decision == MatchDecision.KnockedOut -> "Knocked Out!"

            gauntletProgress.isFinalMatch -> "Gauntlet Complete!"

            else ->
                "Match ${gauntletProgress.currentMatch} of ${gauntletProgress.matchesPerGauntlet}"
        }
    val message =
        when {
            decision == MatchDecision.KnockedOut ->
                "You've been knocked out. Would you like to stay and observe (joining the next match) or leave the arena?"

            gauntletProgress.isFinalMatch ->
                "You've finished all ${gauntletProgress.matchesPerGauntlet} matches of the gauntlet. Go again or leave the arena?"

            else ->
                "Match ${gauntletProgress.currentMatch} of ${gauntletProgress.matchesPerGauntlet} complete. Ready for the next match?"
        }
    val confirmText =
        when {
            decision == MatchDecision.KnockedOut -> "Observe"
            gauntletProgress.isFinalMatch -> "Go Again"
            else -> "Continue"
        }

    AlertDialog(
        onDismissRequest = actions.onStayForAnotherMatch,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = actions.onStayForAnotherMatch) { Text(text = confirmText) }
        },
        dismissButton =
            if (isMatchFinishedMidGauntlet) {
                null
            } else {
                { TextButton(onClick = actions.onConfirmLeaveArena) { Text(text = "Leave") } }
            },
    )
}

@Preview
@Composable
private fun MatchDecisionDialogKnockoutPreview() {
    SorcerersAndSpiritsTheme {
        MatchDecisionDialog(
            decision = MatchDecision.KnockedOut,
            gauntletProgress = GauntletProgress(currentMatch = 1),
        )
    }
}

@Preview
@Composable
private fun MatchDecisionDialogMidGauntletPreview() {
    SorcerersAndSpiritsTheme {
        MatchDecisionDialog(
            decision = MatchDecision.MatchFinished,
            gauntletProgress = GauntletProgress(currentMatch = 1),
        )
    }
}

@Preview
@Composable
private fun MatchDecisionDialogFinalMatchPreview() {
    SorcerersAndSpiritsTheme {
        MatchDecisionDialog(
            decision = MatchDecision.MatchFinished,
            gauntletProgress = GauntletProgress(currentMatch = 3),
        )
    }
}
