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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.MenuTopBar
import com.linklabs.sorcerersandspirits.ui.main.navigation.LocalSignOut
import com.linklabs.sorcerersandspirits.ui.main.ui.SignInMethodsSection
import com.linklabs.sorcerersandspirits.ui.screens.profile.ClassStatsState
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileEvent
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.profile.ProfileState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
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
import sorcerersandspirits.composeapp.generated.resources.profile_name_update_error
import sorcerersandspirits.composeapp.generated.resources.profile_name_update_success
import sorcerersandspirits.composeapp.generated.resources.profile_save
import sorcerersandspirits.composeapp.generated.resources.profile_sign_out
import sorcerersandspirits.composeapp.generated.resources.profile_sign_out_confirm_button
import sorcerersandspirits.composeapp.generated.resources.profile_sign_out_confirm_message
import sorcerersandspirits.composeapp.generated.resources.profile_sign_out_confirm_title
import sorcerersandspirits.composeapp.generated.resources.profile_stats_achievements
import sorcerersandspirits.composeapp.generated.resources.profile_stats_achievements_progress
import sorcerersandspirits.composeapp.generated.resources.profile_stats_class_record
import sorcerersandspirits.composeapp.generated.resources.profile_stats_empty
import sorcerersandspirits.composeapp.generated.resources.profile_stats_gauntlets
import sorcerersandspirits.composeapp.generated.resources.profile_stats_knockouts
import sorcerersandspirits.composeapp.generated.resources.profile_stats_matches
import sorcerersandspirits.composeapp.generated.resources.profile_stats_per_class_title
import sorcerersandspirits.composeapp.generated.resources.profile_stats_rating
import sorcerersandspirits.composeapp.generated.resources.profile_stats_record
import sorcerersandspirits.composeapp.generated.resources.profile_title
import sorcerersandspirits.composeapp.generated.resources.sign_in_name_taken_message

@Composable
internal fun ProfileScreen(
    targetUserId: String?,
    onProfileDeleted: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    viewModel: ProfileScreenViewModel =
        koinViewModel(
            parameters = { parametersOf(ProfileScreenViewModelArguments(targetUserId = targetUserId)) },
        ),
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val nameUpdateSuccessMessage = stringResource(resource = Res.string.profile_name_update_success)
    val nameTakenMessage = stringResource(resource = Res.string.sign_in_name_taken_message)
    val nameUpdateErrorMessage = stringResource(resource = Res.string.profile_name_update_error)

    LaunchedEffect(Unit) {
        viewModel.eventChannelFlow.collect { event ->
            when (event) {
                is ProfileEvent.ProfileDeleted -> onProfileDeleted()

                is ProfileEvent.DisplayNameUpdated -> {
                    val message =
                        when (event.result) {
                            DisplayNameResult.SUCCESS -> nameUpdateSuccessMessage
                            DisplayNameResult.NAME_TAKEN -> nameTakenMessage
                            DisplayNameResult.ERROR -> nameUpdateErrorMessage
                        }
                    // Shown on its own scope so a lingering snackbar never stalls later events.
                    coroutineScope.launch { snackbarHostState.showSnackbar(message = message) }
                }
            }
        }
    }

    val uiState by viewModel.uiStateFlow.collectAsState()
    val linkResult by viewModel.linkResultFlow.collectAsState()
    val canSignOut by viewModel.canSignOutFlow.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { MenuTopBar(title = stringResource(resource = Res.string.profile_title)) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        ProfileScreen(
            uiState = uiState,
            isReadOnly = viewModel.isReadOnly,
            canSignOut = canSignOut,
            linkResult = linkResult,
            onUpdateDisplayName = viewModel::onUpdateDisplayName,
            onDeleteProfile = viewModel::onDeleteProfile,
            onNavigateToAchievements = onNavigateToAchievements,
            onLinkWithSocialMethod = viewModel::onLinkWithSocialMethod,
            onLinkWithEmail = viewModel::onLinkWithEmail,
            modifier = Modifier.padding(paddingValues = paddingValues),
        )
    }
}

@Composable
private fun ProfileScreen(
    uiState: UiState<ProfileState>,
    isReadOnly: Boolean,
    canSignOut: Boolean,
    linkResult: LinkAccountResult?,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onNavigateToAchievements: () -> Unit,
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
                isReadOnly = isReadOnly,
                canSignOut = canSignOut,
                linkResult = linkResult,
                onUpdateDisplayName = onUpdateDisplayName,
                onDeleteProfile = onDeleteProfile,
                onNavigateToAchievements = onNavigateToAchievements,
                onLinkWithSocialMethod = onLinkWithSocialMethod,
                onLinkWithEmail = onLinkWithEmail,
                modifier = modifier,
            )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    isReadOnly: Boolean,
    canSignOut: Boolean,
    linkResult: LinkAccountResult?,
    onUpdateDisplayName: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onLinkWithSocialMethod: (SignInMethod) -> Unit,
    onLinkWithEmail: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDeleteConfirmationShown by rememberSaveable { mutableStateOf(value = false) }
    var isSignOutConfirmationShown by rememberSaveable { mutableStateOf(value = false) }
    val signOut = LocalSignOut.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        item {
            if (isReadOnly) {
                DisplayName(displayName = state.displayName)
            } else {
                DisplayNameEditor(
                    displayName = state.displayName,
                    onUpdateDisplayName = onUpdateDisplayName,
                )
            }
        }

        item {
            StatsSection(
                state = state,
                onNavigateToAchievements =
                if (isReadOnly) null else onNavigateToAchievements,
            )
        }

        if (!isReadOnly) {
            item {
                val isLinkSuccess = linkResult == LinkAccountResult.SUCCESS
                SignInMethodsSection(
                    onSocialMethodClick = onLinkWithSocialMethod,
                    onEmailLink = onLinkWithEmail,
                    statusMessage = linkStatusMessage(result = linkResult),
                    isStatusError = !isLinkSuccess,
                )
            }

            if (canSignOut) {
                item {
                    AppButton(
                        onClick = { isSignOutConfirmationShown = true },
                        borderColor = MaterialTheme.colorScheme.outline,
                    ) {
                        Text(text = stringResource(resource = Res.string.profile_sign_out))
                    }
                }
            }

            item {
                AppButton(
                    onClick = { isDeleteConfirmationShown = true },
                    borderColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.error,
                ) {
                    Text(text = stringResource(resource = Res.string.profile_delete))
                }
            }
        }
    }

    if (isSignOutConfirmationShown) {
        SignOutConfirmationDialog(
            onConfirm = {
                isSignOutConfirmationShown = false
                signOut()
            },
            onDismiss = { isSignOutConfirmationShown = false },
        )
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
private fun DisplayName(displayName: String, modifier: Modifier = Modifier) {
    Text(
        text = displayName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.fillMaxWidth(),
    )
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
            AppButton(
                onClick = { onUpdateDisplayName(pendingDisplayName) },
                isEnabled = isDisplayNameChanged,
            ) {
                Text(text = stringResource(resource = Res.string.profile_save))
            }

            Spacer(modifier = Modifier.width(width = 8.dp))

            AppButton(
                onClick = { pendingDisplayName = displayName },
                isEnabled = isDisplayNameChanged,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Text(text = stringResource(resource = Res.string.profile_cancel))
            }
        }
    }
}

@Composable
private fun StatsSection(
    state: ProfileState,
    onNavigateToAchievements: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        RatingCard(rating = state.rating)

        AchievementsCard(
            unlocked = state.achievementsUnlocked,
            total = state.achievementsTotal,
            onClick = onNavigateToAchievements,
        )

        if (state.hasRecord) {
            OverallRecordCard(overall = state.overall)
            if (state.perClass.isNotEmpty()) PerClassCard(perClass = state.perClass)
        } else {
            EmptyRecordCard()
        }
    }
}

@Composable
private fun RatingCard(rating: Long, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            StatLabel(text = stringResource(resource = Res.string.profile_stats_rating))
            Text(text = rating.toString(), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun AchievementsCard(
    unlocked: Int,
    total: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            AchievementsCardContent(unlocked = unlocked, total = total)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            AchievementsCardContent(unlocked = unlocked, total = total)
        }
    }
}

@Composable
private fun AchievementsCardContent(unlocked: Int, total: Int) {
    Column(modifier = Modifier.padding(all = 16.dp)) {
        StatLabel(text = stringResource(resource = Res.string.profile_stats_achievements))
        Text(
            text =
            stringResource(
                resource = Res.string.profile_stats_achievements_progress,
                unlocked,
                total,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OverallRecordCard(overall: GameStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            LabeledStat(
                label = stringResource(resource = Res.string.profile_stats_matches),
                value =
                stringResource(
                    resource = Res.string.profile_stats_record,
                    overall.matchesWon,
                    overall.matchesLost,
                    overall.matchesPlayed,
                ),
            )
            LabeledStat(
                label = stringResource(resource = Res.string.profile_stats_gauntlets),
                value =
                stringResource(
                    resource = Res.string.profile_stats_record,
                    overall.gauntletsWon,
                    overall.gauntletsLost,
                    overall.gauntletsPlayed,
                ),
            )
            LabeledStat(
                label = stringResource(resource = Res.string.profile_stats_knockouts),
                value = overall.knockoutsDealt.toString(),
            )
        }
    }
}

@Composable
private fun PerClassCard(perClass: List<ClassStatsState>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(
                text = stringResource(resource = Res.string.profile_stats_per_class_title),
                style = MaterialTheme.typography.titleMedium,
            )

            perClass.forEach { classStats ->
                LabeledStat(
                    label = classStats.classType.name,
                    value =
                    stringResource(
                        resource = Res.string.profile_stats_class_record,
                        classStats.stats.matchesWon,
                        classStats.stats.gauntletsWon,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(resource = Res.string.profile_stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(all = 16.dp),
        )
    }
}

@Composable
private fun LabeledStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        StatLabel(text = label)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Unlike the first-launch dialog (which navigates away on success), the player stays on this screen
 * afterwards, so SUCCESS gets its own confirmation message. CANCELLED is the player's own choice.
 */
@Composable
private fun linkStatusMessage(result: LinkAccountResult?): String? = when (result) {
    LinkAccountResult.SUCCESS -> stringResource(resource = Res.string.link_account_success)

    LinkAccountResult.CREDENTIAL_IN_USE ->
        stringResource(resource = Res.string.link_account_error_credential_in_use)

    LinkAccountResult.INVALID_CREDENTIAL ->
        stringResource(resource = Res.string.link_account_error_invalid_credential)

    LinkAccountResult.NOT_AVAILABLE ->
        stringResource(resource = Res.string.link_account_error_unavailable)

    LinkAccountResult.NO_CURRENT_USER,
    LinkAccountResult.ERROR,
    -> stringResource(resource = Res.string.link_account_error_generic)

    LinkAccountResult.CANCELLED,
    null,
    -> null
}

@Composable
private fun SignOutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(resource = Res.string.profile_sign_out_confirm_title))
        },
        text = {
            Text(text = stringResource(resource = Res.string.profile_sign_out_confirm_message))
        },
        confirmButton = {
            AppButton(onClick = onConfirm, borderColor = MaterialTheme.colorScheme.outline) {
                Text(text = stringResource(resource = Res.string.profile_sign_out_confirm_button))
            }
        },
        dismissButton = {
            AppButton(onClick = onDismiss, borderColor = MaterialTheme.colorScheme.outline) {
                Text(text = stringResource(resource = Res.string.profile_cancel))
            }
        },
    )
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
            AppButton(
                onClick = onConfirm,
                borderColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.error,
            ) {
                Text(text = stringResource(resource = Res.string.profile_delete_confirm_button))
            }
        },
        dismissButton = {
            AppButton(onClick = onDismiss, borderColor = MaterialTheme.colorScheme.outline) {
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
            uiState =
            UiState.Loaded(
                data =
                ProfileState(
                    displayName = "Gandalf",
                    rating = 1_240L,
                    overall =
                    GameStats(
                        matchesPlayed = 40,
                        matchesWon = 22,
                        matchesLost = 18,
                        gauntletsPlayed = 12,
                        gauntletsWon = 7,
                        gauntletsLost = 5,
                        knockoutsDealt = 63,
                    ),
                    perClass =
                    listOf(
                        ClassStatsState(
                            classType = CharacterClassType.Mage,
                            stats = GameStats(matchesWon = 14, gauntletsWon = 4),
                        ),
                        ClassStatsState(
                            classType = CharacterClassType.Paladin,
                            stats = GameStats(matchesWon = 8, gauntletsWon = 3),
                        ),
                    ),
                    achievementsUnlocked = 2,
                    achievementsTotal = 4,
                ),
            ),
            isReadOnly = false,
            canSignOut = true,
            linkResult = null,
            onUpdateDisplayName = {},
            onDeleteProfile = {},
            onNavigateToAchievements = {},
            onLinkWithSocialMethod = {},
            onLinkWithEmail = { _, _ -> },
        )
    }
}
