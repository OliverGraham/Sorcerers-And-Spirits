package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.MatchDecision
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun MatchDecisionDialog(decision: MatchDecision) {
    val actions = ArenaActionsProvider.current

    val title =
        when (decision) {
            MatchDecision.MatchFinished -> "Match Finished!"
            MatchDecision.KnockedOut -> "Knocked Out!"
        }
    val message =
        when (decision) {
            MatchDecision.MatchFinished ->
                "The match has ended. Would you like to stay for another match or leave the arena?"

            MatchDecision.KnockedOut ->
                "You've been knocked out. Would you like to stay and observe (joining the next match) or leave the arena?"
        }
    val confirmText =
        when (decision) {
            MatchDecision.MatchFinished -> "Stay"
            MatchDecision.KnockedOut -> "Observe"
        }

    AlertDialog(
        onDismissRequest = actions.onStayForAnotherMatch,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = actions.onStayForAnotherMatch) { Text(text = confirmText) }
        },
        dismissButton = {
            TextButton(onClick = actions.onConfirmLeaveArena) { Text(text = "Leave") }
        },
    )
}

@Preview
@Composable
private fun MatchDecisionDialogKnockoutPreview() {
    SorcerersAndSpiritsTheme { MatchDecisionDialog(decision = MatchDecision.KnockedOut) }
}

@Preview
@Composable
private fun MatchDecisionDialogFinishedPreview() {
    SorcerersAndSpiritsTheme { MatchDecisionDialog(decision = MatchDecision.MatchFinished) }
}
