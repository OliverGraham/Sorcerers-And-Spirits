package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_menu

// TODO: Improve

/** Location where users can challenge other users to fight in the Arena. */
@Composable
internal fun TheBar(
    userTraveler: TravelerState,
    otherTravelers: List<TravelerState>,
    modifier: Modifier = Modifier,
) {
    var selectedTravelers by remember { mutableStateOf(setOf<TravelerState>()) }

    Column(modifier = modifier.navigationBarsPadding()) {
        BarFolk(
            otherTravelers = otherTravelers,
            selectedTravelers = selectedTravelers,
            updateSelectedTravelers = { selectedTravelers = it },
            modifier = Modifier.weight(weight = 1f),
        )

        ChallengeButton(
            userTraveler = userTraveler,
            selectedTravelers = selectedTravelers,
            resetSelectedTravelers = { selectedTravelers = emptySet() },
        )
    }
}

@Composable
private fun BarFolk(
    otherTravelers: List<TravelerState>,
    selectedTravelers: Set<TravelerState>,
    updateSelectedTravelers: (Set<TravelerState>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDialog = TavernActionsProvider.current.onShowArenaChallengeDialog
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        item {
            // TODO: Change UI - should say other players to challenge or something
            //  Idea - UI in settings to show every player playing right now, and their status and
            //  such

            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = showDialog,
                    modifier = Modifier.align(alignment = Alignment.TopStart).size(size = 48.dp),
                ) {
                    // TODO: new icon
                    Icon(
                        painter = painterResource(resource = Res.drawable.ic_menu),
                        contentDescription = "",
                    )
                }

                AnimatedContent(
                    targetState = otherTravelers.size,
                    modifier = Modifier.align(alignment = Alignment.Center),
                ) { count ->
                    Text(
                        text = "Travelers in Tavern: $count",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = "Challenge people to fight in The Arena:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(items = otherTravelers) { travelerState ->
            travelerState.ToCard(
                isSelected = travelerState in selectedTravelers,
                onClick = {
                    val updatedTravelers =
                        if (travelerState in selectedTravelers) {
                            selectedTravelers - travelerState
                        } else {
                            selectedTravelers + travelerState
                        }
                    updateSelectedTravelers(updatedTravelers)
                },
            )
        }
    }
}

@Composable
private fun ChallengeButton(
    userTraveler: TravelerState,
    selectedTravelers: Set<TravelerState>,
    resetSelectedTravelers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = selectedTravelers.isNotEmpty(), modifier = modifier) {
        val onSendChallengerNotifications =
            TavernActionsProvider.current.onSendChallengerNotifications

        Button(
            onClick = {
                onSendChallengerNotifications(userTraveler, selectedTravelers)
                resetSelectedTravelers()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(text = "Challenge (${selectedTravelers.size})")
        }
    }
}

// TODO: Update styling
@Composable
private fun TravelerState.ToCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (isThisTravelerTheUser) "$displayName (You)" else displayName

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isThisTravelerTheUser -> MaterialTheme.colorScheme.primaryContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                contentColor =
                    if (isThisTravelerTheUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            ),
    ) {
        Box(
            modifier =
                Modifier.clickable(enabled = !isThisTravelerTheUser) { onClick() }
                    .padding(24.dp)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight =
                    if (isThisTravelerTheUser || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TheBarPreview() {
    TheBar(
        userTraveler =
            TravelerState(
                userId = "-1",
                displayName = "User",
                isThisTravelerTheUser = true,
                tavernStatus = TavernStatus.Available,
            ),
        otherTravelers =
            buildList {
                repeat(times = 10) {
                    add(
                        TravelerState(
                            userId = it.toString(),
                            displayName = "Traveler $it",
                            isThisTravelerTheUser = false,
                            tavernStatus = TavernStatus.Available,
                        )
                    )
                }
            },
        modifier = Modifier.fillMaxSize(),
    )
}
