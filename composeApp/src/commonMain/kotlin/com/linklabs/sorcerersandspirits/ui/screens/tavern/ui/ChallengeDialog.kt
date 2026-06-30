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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleInstigatorChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleRecipientChallengeState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
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
        onDismissRequest = ChallengeActionsProvider.current.onHideChallengeDialog,
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            challengeState.DialogContent()
        }
    }
}

@Composable
private fun ChallengeState.DialogContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        val isThisUserTheInstigator = isThisUserTheInstigator
        HeaderSection(
            formattedTimeRemaining = formattedTimeRemaining,
            isLessThanTenSeconds = isLessThanTenSeconds,
            instigatorDisplayName =
                instigator?.displayName
                    ?: stringResource(resource = Res.string.challenge_dialog_default_instigator),
            isThisUserTheInstigator = isThisUserTheInstigator,
        )

        ChallengersList(challengerStates = challengerStates)

        if (!isThisUserTheInstigator) {
            ActionButtons(challengeId = challengeId)
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
        horizontalArrangement =
            Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val onDeclineChallenge = ChallengeActionsProvider.current.onDeclineChallenge
        AppButton(
            onClick = { onDeclineChallenge(challengeId) },
            borderColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(text = stringResource(resource = Res.string.challenge_dialog_button_decline))
        }

        val onAcceptChallenge = ChallengeActionsProvider.current.onAcceptChallenge
        AppButton(onClick = { onAcceptChallenge(challengeId) }) {
            Text(text = stringResource(resource = Res.string.challenge_dialog_button_accept))
        }
    }
}

@Preview
@Composable
private fun ChallengeDialogInstigatorPreview() {
    SorcerersAndSpiritsTheme { ChallengeDialog(challengeState = exampleInstigatorChallengeState) }
}

@Preview
@Composable
private fun ChallengeDialogRecipientPreview() {
    SorcerersAndSpiritsTheme { ChallengeDialog(challengeState = exampleRecipientChallengeState) }
}
