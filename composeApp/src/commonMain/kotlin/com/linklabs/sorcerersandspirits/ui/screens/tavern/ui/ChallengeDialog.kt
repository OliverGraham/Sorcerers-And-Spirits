package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.ui.common.AnimatedSeconds
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.formatSecondsRemaining
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleInstigatorChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleRecipientChallengeState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_arena_name
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_awaiting_responses
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_button_accept
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_button_decline
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_challenged_by
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_default_instigator
import sorcerersandspirits.composeapp.generated.resources.challenge_dialog_title
import sorcerersandspirits.composeapp.generated.resources.challenge_status_accepted
import sorcerersandspirits.composeapp.generated.resources.challenge_status_declined
import sorcerersandspirits.composeapp.generated.resources.challenge_status_pending

private val ChallengeStatusAcceptedContainer = Color(0xFF2E7D32)
private val ChallengeStatusAcceptedContent = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeDialog(challengeState: ChallengeState, modifier: Modifier = Modifier) {
    BasicAlertDialog(
        onDismissRequest = ChallengeActionsProvider.current.onHideChallengeDialog,
        modifier = modifier,
    ) {
        ChallengeDialogSurface(challengeState = challengeState)
    }
}

@Composable
private fun ChallengeDialogSurface(challengeState: ChallengeState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 300.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = AlertDialogDefaults.containerColor,
    ) {
        challengeState.DialogContent()
    }
}

@Composable
private fun ChallengeState.DialogContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
    ) {
        val isThisUserTheInstigator = isThisUserTheInstigator
        HeaderSection(
            secondsRemaining = secondsRemaining,
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
    secondsRemaining: Int,
    isLessThanTenSeconds: Boolean,
    instigatorDisplayName: String,
    isThisUserTheInstigator: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(resource = Res.string.challenge_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(weight = 1f),
            )

            TimerChip(
                secondsRemaining = secondsRemaining,
                isLessThanTenSeconds = isLessThanTenSeconds,
            )
        }

        ArenaTitle()

        val subtitle =
            if (isThisUserTheInstigator) {
                stringResource(resource = Res.string.challenge_dialog_awaiting_responses)
            } else {
                stringResource(
                    resource = Res.string.challenge_dialog_challenged_by,
                    instigatorDisplayName,
                )
            }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArenaTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(resource = Res.string.challenge_dialog_arena_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimerChip(
    secondsRemaining: Int,
    isLessThanTenSeconds: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor =
        if (isLessThanTenSeconds) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    AnimatedSeconds(
        seconds = secondsRemaining,
        modifier =
        modifier
            .clip(shape = CircleShape)
            .border(width = 1.5.dp, color = accentColor, shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) { animatedSeconds ->
        Text(
            text = formatSecondsRemaining(secondsRemaining = animatedSeconds),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
    }
}

@Composable
private fun ChallengersList(
    challengerStates: List<ChallengerState>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
    ) {
        items(items = challengerStates, key = { it.userId }) { challengerState ->
            ChallengerRow(challengerState = challengerState)
        }
    }
}

@Composable
private fun ChallengerRow(challengerState: ChallengerState, modifier: Modifier = Modifier) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 14.dp))
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = challengerState.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f),
        )

        StatusChip(status = challengerState.status)
    }
}

@Composable
private fun StatusChip(status: ChallengerStatus, modifier: Modifier = Modifier) {
    when (status) {
        ChallengerStatus.Accepted -> AcceptedStatusChip(modifier = modifier)
        ChallengerStatus.Pending -> PendingStatusChip(modifier = modifier)
        ChallengerStatus.Declined -> DeclinedStatusChip(modifier = modifier)
    }
}

@Composable
private fun AcceptedStatusChip(modifier: Modifier = Modifier) {
    StatusChipContent(
        label = stringResource(resource = Res.string.challenge_status_accepted),
        containerColor = ChallengeStatusAcceptedContainer,
        contentColor = ChallengeStatusAcceptedContent,
        modifier = modifier,
    )
}

@Composable
private fun PendingStatusChip(modifier: Modifier = Modifier) {
    StatusChipContent(
        label = stringResource(resource = Res.string.challenge_status_pending),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier,
    )
}

@Composable
private fun DeclinedStatusChip(modifier: Modifier = Modifier) {
    StatusChipContent(
        label = stringResource(resource = Res.string.challenge_status_declined),
        containerColor = MaterialTheme.colorScheme.outlineVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun StatusChipContent(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .clip(shape = CircleShape)
            .background(color = containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier.size(size = 8.dp).clip(shape = CircleShape).background(color = contentColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun ActionButtons(challengeId: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
        Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally),
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
    SorcerersAndSpiritsTheme {
        ChallengeDialogSurface(challengeState = exampleInstigatorChallengeState)
    }
}

@Preview
@Composable
private fun ChallengeDialogRecipientPreview() {
    SorcerersAndSpiritsTheme {
        ChallengeDialogSurface(challengeState = exampleRecipientChallengeState)
    }
}
