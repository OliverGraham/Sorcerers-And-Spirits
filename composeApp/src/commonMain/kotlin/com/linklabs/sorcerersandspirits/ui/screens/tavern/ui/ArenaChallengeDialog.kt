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
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ArenaChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState

// TODO: Update the AI Slop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArenaChallengeDialog(
    arenaChallenge: ArenaChallengeState,
    userTraveler: TravelerState,
    modifier: Modifier = Modifier,
) {
    val onAcceptChallenge = TavernActionsProvider.current.onAcceptArenaChallenge
    val onDeclineChallenge = TavernActionsProvider.current.onDeclineArenaChallenge

    BasicAlertDialog(
        onDismissRequest = { onDeclineChallenge(arenaChallenge, userTraveler) },
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
                // Header section
                Text(text = "Arena Challenge", style = MaterialTheme.typography.headlineSmall)

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
                    val allTravelerStates = arenaChallenge.travelerStates

                    items(allTravelerStates, key = { it.id }) { travelerState ->
                        ParticipantRow(travelerState)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onDeclineChallenge(arenaChallenge, userTraveler) }) {
                        Text("Decline")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onAcceptChallenge(arenaChallenge, userTraveler) }) {
                        Text("Accept Challenge")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: TravelerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(participant.displayName, style = MaterialTheme.typography.bodyLarge)

        // Placeholder for Status (e.g. Accepted/Pending)
        // You'll likely want to update TravelerState to include a 'status' field
        // as we discussed in the TavernState update.
        Text(text = participant.status.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}
