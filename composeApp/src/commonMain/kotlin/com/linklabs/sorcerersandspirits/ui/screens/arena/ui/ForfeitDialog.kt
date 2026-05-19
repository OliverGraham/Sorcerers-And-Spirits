package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun ForfeitDialog() {
    val actions = ArenaActionsProvider.current

    AlertDialog(
        onDismissRequest = actions.onDismissForfeitDialog,
        title = { Text(text = "Forfeit Game?") },
        text = {
            Text(
                text =
                    "Leaving now will cause you to forfeit the current game. Are you sure you want to leave?"
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmLeaveArena) { Text(text = "Forfeit") }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissForfeitDialog) { Text(text = "Cancel") }
        },
    )
}

@Preview
@Composable
private fun ForfeitDialogPreview() {
    SorcerersAndSpiritsTheme { ForfeitDialog() }
}
