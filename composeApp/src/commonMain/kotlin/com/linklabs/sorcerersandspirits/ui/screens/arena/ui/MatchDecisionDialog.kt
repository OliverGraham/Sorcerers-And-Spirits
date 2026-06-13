package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun MatchDecisionDialog(isMatchFinished: Boolean, isUserPlayerKnockedOut: Boolean) {
    val actions = ArenaActionsProvider.current

    val title = if (isMatchFinished) "Match Finished!" else "Knocked Out!"
    val message =
        if (isMatchFinished) {
            "The match has ended. Would you like to stay for another match or leave the arena?"
        } else {
            "You've been knocked out. Would you like to stay and observe (joining the next match) or leave the arena?"
        }
    val confirmText = if (isMatchFinished) "Stay" else "Observe"

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
    SorcerersAndSpiritsTheme {
        MatchDecisionDialog(isMatchFinished = false, isUserPlayerKnockedOut = true)
    }
}

@Preview
@Composable
private fun MatchDecisionDialogFinishedPreview() {
    SorcerersAndSpiritsTheme {
        MatchDecisionDialog(isMatchFinished = true, isUserPlayerKnockedOut = false)
    }
}
