package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.link_account_apple
import sorcerersandspirits.composeapp.generated.resources.link_account_email
import sorcerersandspirits.composeapp.generated.resources.link_account_email_label
import sorcerersandspirits.composeapp.generated.resources.link_account_email_submit
import sorcerersandspirits.composeapp.generated.resources.link_account_google
import sorcerersandspirits.composeapp.generated.resources.link_account_optional_note
import sorcerersandspirits.composeapp.generated.resources.link_account_password_label

/**
 * Optional account-linking affordances (Google, Apple, email). Reused by the first-launch
 * [SignInNameDialog] and the ProfileScreen (SS-064): both delegate the same domain
 * `LinkAccountUseCase`, so this stays purely presentational — it only emits the chosen method and
 * the typed email credentials, and renders whatever status the caller passes back.
 */
@Composable
fun SignInMethodsSection(
    onSocialMethodClick: (SignInMethod) -> Unit,
    onEmailLink: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    isStatusError: Boolean = true,
) {
    var isEmailFormExpanded by remember { mutableStateOf(value = false) }
    var email by remember { mutableStateOf(value = "") }
    var password by remember { mutableStateOf(value = "") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = stringResource(resource = Res.string.link_account_optional_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { onSocialMethodClick(SignInMethod.GOOGLE) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(resource = Res.string.link_account_google))
        }

        OutlinedButton(
            onClick = { onSocialMethodClick(SignInMethod.APPLE) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(resource = Res.string.link_account_apple))
        }

        OutlinedButton(
            onClick = { isEmailFormExpanded = !isEmailFormExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(resource = Res.string.link_account_email))
        }

        if (isEmailFormExpanded) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(resource = Res.string.link_account_email_label))
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(resource = Res.string.link_account_password_label))
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            val isEmailSubmittable = email.isNotBlank() && password.isNotBlank()
            Button(
                onClick = { onEmailLink(email, password) },
                enabled = isEmailSubmittable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(resource = Res.string.link_account_email_submit))
            }
        }

        if (statusMessage != null) {
            val statusColor =
                if (isStatusError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            Text(
                text = statusMessage,
                color = statusColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun SignInMethodsSectionPreview() {
    SignInMethodsSection(onSocialMethodClick = {}, onEmailLink = { _, _ -> })
}
