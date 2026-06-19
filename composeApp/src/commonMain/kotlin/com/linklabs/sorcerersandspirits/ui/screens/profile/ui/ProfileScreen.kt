package com.linklabs.sorcerersandspirits.ui.screens.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInMethodsSection
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileEvent
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.link_account_error_credential_in_use
import sorcerersandspirits.composeapp.generated.resources.link_account_error_generic
import sorcerersandspirits.composeapp.generated.resources.link_account_error_invalid_credential
import sorcerersandspirits.composeapp.generated.resources.link_account_error_unavailable
import sorcerersandspirits.composeapp.generated.resources.link_account_success
import sorcerersandspirits.composeapp.generated.resources.profile_cancel
import sorcerersandspirits.composeapp.generated.resources.profile_delete
import sorcerersandspirits.composeapp.generated.resources.profile_delete_confirm_button
import sorcerersandspirits.composeapp.generated.resources.profile_delete_confirm_message
import sorcerersandspirits.composeapp.generated.resources.profile_delete_confirm_title
import sorcerersandspirits.composeapp.generated.resources.profile_display_name_label
import sorcerersandspirits.composeapp.generated.resources.profile_save
import sorcerersandspirits.composeapp.generated.resources.profile_stats_placeholder
import sorcerersandspirits.composeapp.generated.resources.profile_stats_title
import sorcerersandspirits.composeapp.generated.resources.profile_title

@Composable
internal fun ProfileScreen(
    onProfileDeleted: () -> Unit,
    viewModel: ProfileScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewModel.eventChannelFlow.collect { event ->
            when (event) {
                is ProfileEvent.ProfileDeleted -> onProfileDeleted()
            }
        }
    }

    val uiState by viewModel.uiStateFlow.collectAsState()
    val linkResult by viewModel.linkResultFlow.collectAsState()

    ProfileScreen(
        uiState = uiState,
        linkResult = linkResult,
        onUpdateDisplayName = viewModel::onUpdateDisplayName,
        onDeleteProfile = viewModel::onDeleteProfile,
        onLinkWithSocialMethod = viewModel::onLinkWithSocialMethod,
        onLinkWithEmail = viewModel::onLinkWithEmail,
        modifier = modifier,
    )
}

@Composable
private fun ProfileScreen(
    uiState: UiState<ProfileState>,
    linkResult: LinkAccountResult?,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onLinkWithSocialMethod: (SignInMethod) -> Unit,
    onLinkWithEmail: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = {}, modifier = modifier)

        is UiState.Loading -> LoadingIndicator(modifier = modifier)

        is UiState.Loaded ->
            ProfileContent(
                state = uiState.data,
                linkResult = linkResult,
                onUpdateDisplayName = onUpdateDisplayName,
                onDeleteProfile = onDeleteProfile,
                onLinkWithSocialMethod = onLinkWithSocialMethod,
                onLinkWithEmail = onLinkWithEmail,
                modifier = modifier,
            )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    linkResult: LinkAccountResult?,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onLinkWithSocialMethod: (SignInMethod) -> Unit,
    onLinkWithEmail: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDeleteConfirmationShown by rememberSaveable { mutableStateOf(value = false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        item {
            Text(
                text = stringResource(resource = Res.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            DisplayNameEditor(
                displayName = state.displayName,
                onUpdateDisplayName = onUpdateDisplayName,
            )
        }

        item { StatsPlaceholder() }

        item {
            val isLinkSuccess = linkResult == LinkAccountResult.SUCCESS
            SignInMethodsSection(
                onSocialMethodClick = onLinkWithSocialMethod,
                onEmailLink = onLinkWithEmail,
                statusMessage = linkStatusMessage(result = linkResult),
                isStatusError = !isLinkSuccess,
            )
        }

        item {
            OutlinedButton(
                onClick = { isDeleteConfirmationShown = true },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
            ) {
                Text(text = stringResource(resource = Res.string.profile_delete))
            }
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
private fun DisplayNameEditor(
    displayName: String,
    onUpdateDisplayName: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDisplayName by rememberSaveable { mutableStateOf(value = displayName) }

    // Keep the field in sync if the saved name changes from outside (e.g. a successful update).
    LaunchedEffect(displayName) { pendingDisplayName = displayName }

    val isDisplayNameChanged = pendingDisplayName != displayName

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        OutlinedTextField(
            value = pendingDisplayName,
            onValueChange = { pendingDisplayName = it },
            label = {
                Text(text = stringResource(resource = Res.string.profile_display_name_label))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row {
            Button(
                onClick = { onUpdateDisplayName(pendingDisplayName) },
                enabled = isDisplayNameChanged,
            ) {
                Text(text = stringResource(resource = Res.string.profile_save))
            }

            Spacer(modifier = Modifier.width(width = 8.dp))

            Button(onClick = { pendingDisplayName = displayName }, enabled = isDisplayNameChanged) {
                Text(text = stringResource(resource = Res.string.profile_cancel))
            }
        }
    }
}

// TODO(SS-064): Replace with the player's real game statistics once stat handling lands
//  (SS-072 / SS-074).
@Composable
private fun StatsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = stringResource(resource = Res.string.profile_stats_title),
            style = MaterialTheme.typography.titleLarge,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(resource = Res.string.profile_stats_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(all = 16.dp),
            )
        }
    }
}

/**
 * Unlike the first-launch dialog (which navigates away on success), the player stays on this screen
 * afterwards, so SUCCESS gets its own confirmation message. CANCELLED is the player's own choice.
 */
@Composable
private fun linkStatusMessage(result: LinkAccountResult?): String? =
    when (result) {
        LinkAccountResult.SUCCESS -> stringResource(resource = Res.string.link_account_success)

        LinkAccountResult.CREDENTIAL_IN_USE ->
            stringResource(resource = Res.string.link_account_error_credential_in_use)

        LinkAccountResult.INVALID_CREDENTIAL ->
            stringResource(resource = Res.string.link_account_error_invalid_credential)

        LinkAccountResult.NOT_AVAILABLE ->
            stringResource(resource = Res.string.link_account_error_unavailable)

        LinkAccountResult.NO_CURRENT_USER,
        LinkAccountResult.ERROR -> stringResource(resource = Res.string.link_account_error_generic)

        LinkAccountResult.CANCELLED,
        null -> null
    }

@Composable
private fun DeleteProfileConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.profile_delete_confirm_title)) },
        text = {
            Text(text = stringResource(resource = Res.string.profile_delete_confirm_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(resource = Res.string.profile_delete_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.profile_cancel))
            }
        },
    )
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    SorcerersAndSpiritsTheme {
        ProfileScreen(
            uiState = UiState.Loaded(data = ProfileState(displayName = "Gandalf")),
            linkResult = null,
            onUpdateDisplayName = {},
            onDeleteProfile = {},
            onLinkWithSocialMethod = {},
            onLinkWithEmail = { _, _ -> },
        )
    }
}
