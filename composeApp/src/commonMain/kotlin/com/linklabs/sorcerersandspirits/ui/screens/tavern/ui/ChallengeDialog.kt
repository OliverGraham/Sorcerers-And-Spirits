package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActionsProvider
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_awaiting_responses
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_button_accept
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_button_decline
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_challenged_by
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_default_instigator
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeDialog(challengeState: ChallengeState, modifier: Modifier = Modifier) {
    BasicAlertDialog(
        onDismissRequest = TavernActionsProvider.current.onHideChallengeDialog,
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                val isThisUserTheInstigator = challengeState.isThisUserTheInstigator
                HeaderSection(
                    formattedTimeRemaining = challengeState.formattedTimeRemaining,
                    isLessThanTenSeconds = challengeState.isLessThanTenSeconds,
                    instigatorDisplayName =
                        challengeState.instigator?.displayName
                            ?: stringResource(
                                resource = Res.string.challenge_dialog_default_instigator
                            ),
                    isThisUserTheInstigator = isThisUserTheInstigator,
                )

                ChallengersList(challengerStates = challengeState.challengerStates)

                if (!isThisUserTheInstigator) {
                    ActionButtons(challengeId = challengeState.challengeId)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    formattedTimeRemaining: String,
    isLessThanTenSeconds: Boolean,
    instigatorDisplayName: String,
    isThisUserTheInstigator: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(resource = Res.string.challenge_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = formattedTimeRemaining,
            style = MaterialTheme.typography.titleMedium,
            color =
                if (isLessThanTenSeconds) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }

    val text =
        if (isThisUserTheInstigator) {
            stringResource(resource = Res.string.challenge_dialog_awaiting_responses)
        } else {
            stringResource(
                resource = Res.string.challenge_dialog_challenged_by,
                instigatorDisplayName,
            )
        }
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ChallengersList(
    challengerStates: List<ChallengerState>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().heightIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        items(items = challengerStates, key = { it.userId }) { challengerState ->
            ChallengerInformation(challengerState = challengerState)
        }
    }
}

@Composable
private fun ChallengerInformation(challengerState: ChallengerState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = challengerState.displayName, style = MaterialTheme.typography.bodyLarge)

        Text(text = challengerState.status.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionButtons(challengeId: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val onDeclineChallenge = TavernActionsProvider.current.onDeclineChallenge
        TextButton(onClick = { onDeclineChallenge(challengeId) }) {
            Text(text = stringResource(resource = Res.string.challenge_dialog_button_decline))
        }

        val onAcceptChallenge = TavernActionsProvider.current.onAcceptChallenge
        Button(onClick = { onAcceptChallenge(challengeId) }) {
            Text(text = stringResource(resource = Res.string.challenge_dialog_button_accept))
        }
    }
}
