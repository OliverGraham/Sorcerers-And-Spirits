package com.linklabs.sorcerersandspirits.ui.screens.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO(SS-064): Placeholder ProfileScreen. Promote to a route + ViewModel, reuse
//  UpdateProfileDisplayNameUseCase for the edit flow, and add per-user stats (SS-021/SS-072).
@Composable
internal fun ProfileScreen(
    displayName: String,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDisplayName by rememberSaveable { mutableStateOf(displayName) }
    var isDeleteConfirmationShown by rememberSaveable { mutableStateOf(false) }

    // Keep local field in sync if displayName changes from outside.
    LaunchedEffect(displayName) { pendingDisplayName = displayName }

    Column(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pendingDisplayName,
            onValueChange = { pendingDisplayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = { onUpdateDisplayName(pendingDisplayName) },
                enabled = pendingDisplayName != displayName,
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { pendingDisplayName = displayName },
                enabled = pendingDisplayName != displayName,
            ) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(height = 32.dp))

        OutlinedButton(
            onClick = { isDeleteConfirmationShown = true },
            colors =
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete Profile")
        }
    }

    if (isDeleteConfirmationShown) {
        DeleteProfileConfirmationDialog(
            onConfirm = {
                isDeleteConfirmationShown = false
                onDeleteProfile()
            },
            onDismiss = { isDeleteConfirmationShown = false },
        )
    }
}

@Composable
private fun DeleteProfileConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Profile?") },
        text = {
            Text(
                "This permanently deletes your profile, including your display name and game " +
                    "stats. This can't be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    SorcerersAndSpiritsTheme {
        ProfileScreen(displayName = "Gandalf", onUpdateDisplayName = {}, onDeleteProfile = {})
    }
}
