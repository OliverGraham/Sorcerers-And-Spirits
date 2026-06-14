package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.sign_in_button_accept
import sorcerersandspirits.composeapp.generated.resources.sign_in_button_random
import sorcerersandspirits.composeapp.generated.resources.sign_in_description
import sorcerersandspirits.composeapp.generated.resources.sign_in_label_name
import sorcerersandspirits.composeapp.generated.resources.sign_in_offline_message
import sorcerersandspirits.composeapp.generated.resources.sign_in_title

@Composable
fun SignInNameDialog(
    displayName: String,
    onRandomNameClick: () -> Unit,
    onAcceptClick: (String) -> Unit,
    isOffline: Boolean = false,
    onDismissRequest: () -> Unit = {},
) {
    var currentDisplayName by remember(key1 = displayName) { mutableStateOf(value = displayName) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(size = 24.dp), tonalElevation = 8.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                Text(
                    text = stringResource(resource = Res.string.sign_in_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = stringResource(resource = Res.string.sign_in_description),
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = currentDisplayName,
                    onValueChange = { currentDisplayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(resource = Res.string.sign_in_label_name))
                    },
                    singleLine = true,
                )

                if (isOffline) {
                    Text(
                        text = stringResource(resource = Res.string.sign_in_offline_message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = onRandomNameClick,
                        modifier = Modifier.weight(weight = 1f),
                    ) {
                        Text(text = stringResource(resource = Res.string.sign_in_button_random))
                    }

                    Button(
                        onClick = { onAcceptClick(currentDisplayName) },
                        enabled = currentDisplayName.isNotBlank(),
                        modifier = Modifier.weight(weight = 1f),
                    ) {
                        Text(text = stringResource(resource = Res.string.sign_in_button_accept))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SignInNameDialogPreview() {
    SignInNameDialog(
        displayName = "A Nameless Traveler",
        onRandomNameClick = {},
        onAcceptClick = {},
    )
}
