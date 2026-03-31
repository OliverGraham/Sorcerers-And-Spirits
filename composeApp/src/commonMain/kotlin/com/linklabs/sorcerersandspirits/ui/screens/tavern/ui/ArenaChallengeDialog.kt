package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

// TODO: Update the AI Slop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArenaChallengeDialog(arenaChallenge: ChallengeState, modifier: Modifier = Modifier) {
    val onAcceptChallenge = TavernActionsProvider.current.onAcceptArenaChallenge
    val onDeclineChallenge = TavernActionsProvider.current.onDeclineArenaChallenge

    BasicAlertDialog(
        onDismissRequest = TavernActionsProvider.current.onHideArenaChallengeDialog,
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header section with Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Arena Challenge", style = MaterialTheme.typography.headlineSmall)

                    arenaChallenge.secondsRemaining?.let { secondsLeft ->
                        val minutes = secondsLeft / 60
                        val seconds = secondsLeft % 60
                        Text(
                            text = "$minutes:${seconds.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                if (secondsLeft <= 10) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        )
                    }
                }

                Text(
                    text = "${arenaChallenge.sender?.displayName} has challenged you!",
                    style = MaterialTheme.typography.bodyMedium,
                )

                // The Participants List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Combine sender and receivers for the UI
                    val allTravelerStates = arenaChallenge.challengerStates

                    items(allTravelerStates, key = { it.userId }) { challengerState ->
                        ParticipantRow(challengerState = challengerState)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onDeclineChallenge(arenaChallenge) }) { Text("Decline") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onAcceptChallenge(arenaChallenge) }) {
                        Text("Accept Challenge")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(challengerState: ChallengerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(challengerState.displayName, style = MaterialTheme.typography.bodyLarge)

        Text(text = challengerState.status.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}
