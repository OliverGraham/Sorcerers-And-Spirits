package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.ui.common.AppButton
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
                "Leaving now will cause you to forfeit the current game. Are you sure you want to leave?",
            )
        },
        confirmButton = {
            AppButton(
                onClick = actions.onConfirmLeaveArena,
                borderColor = MaterialTheme.colorScheme.error,
            ) {
                Text(text = "Forfeit")
            }
        },
        dismissButton = {
            AppButton(
                onClick = actions.onDismissForfeitDialog,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Text(text = "Cancel")
            }
        },
    )
}

@Preview
@Composable
private fun ForfeitDialogPreview() {
    SorcerersAndSpiritsTheme { ForfeitDialog() }
}
