package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO: Fix the slop

@Composable
internal fun KnockoutDialog() {
    val actions = ArenaActionsProvider.current

    AlertDialog(
        onDismissRequest = actions.onStayForAnotherMatch,
        title = { Text(text = "Knocked Out!") },
        text = {
            Text(
                text =
                    "You've been knocked out of the match. Would you like to stay and observe (joining the next match) or leave the arena?"
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onStayForAnotherMatch) { Text(text = "Observe") }
        },
        dismissButton = {
            TextButton(onClick = actions.onConfirmLeaveArena) { Text(text = "Leave") }
        },
    )
}

@Preview
@Composable
private fun KnockoutDialogPreview() {
    SorcerersAndSpiritsTheme { KnockoutDialog() }
}
