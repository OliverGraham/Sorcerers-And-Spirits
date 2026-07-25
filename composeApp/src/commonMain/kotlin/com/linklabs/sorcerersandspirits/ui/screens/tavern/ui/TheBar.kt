package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleOtherTravelers
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview.exampleUserTraveler
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.tavern_challenge_instruction
import sorcerersandspirits.composeapp.generated.resources.tavern_search_no_matches
import sorcerersandspirits.composeapp.generated.resources.tavern_search_travelers_hint
import sorcerersandspirits.composeapp.generated.resources.tavern_traveler_rating
import sorcerersandspirits.composeapp.generated.resources.tavern_travelers_to_challenge

private const val TRAVELERS_IN_TAVERN_COUNT_LABEL = "TravelersInTavernCount"
private const val TRAVELER_CARD_SELECTION_LABEL = "TravelerCardSelection"
private const val CHALLENGE_BUTTON_VISIBILITY_LABEL = "ChallengeButtonVisibility"

/** Location where users can challenge other users to fight in the Arena. */
@Composable
internal fun TheBar(
    userTraveler: TravelerState,
    otherTravelers: List<TravelerState>,
    modifier: Modifier = Modifier,
) {
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    // A traveler can only be challenged while present in the Tavern and available, so selections of
    // travelers who have since left or become busy drop out of the count and the outgoing
    // challenge. Selection is derived from the full list rather than the search-filtered one, so a
    // traveler that is picked and then filtered out of view stays in the outgoing challenge.
    val selectedTravelers =
        otherTravelers
            .filter { traveler ->
                traveler.userId in selectedUserIds && traveler.tavernStatus != TavernStatus.Busy
            }
            .toSet()

    val trimmedQuery = searchQuery.trim()
    val visibleTravelers =
        if (trimmedQuery.isEmpty()) {
            otherTravelers
        } else {
            otherTravelers.filter { traveler ->
                traveler.displayName.contains(other = trimmedQuery, ignoreCase = true)
            }
        }

    Column(modifier = modifier.navigationBarsPadding()) {
        TravelerSearchField(searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it })

        BarFolk(
            visibleTravelers = visibleTravelers,
            travelerCount = otherTravelers.size,
            searchQuery = trimmedQuery,
            selectedUserIds = selectedTravelers.mapTo(mutableSetOf()) { it.userId },
            onToggleTraveler = { traveler ->
                selectedUserIds =
                    if (traveler.userId in selectedUserIds) {
                        selectedUserIds - traveler.userId
                    } else {
                        selectedUserIds + traveler.userId
                    }
            },
            modifier = Modifier.weight(weight = 1f),
        )

        ChallengeButton(
            userTraveler = userTraveler,
            selectedTravelers = selectedTravelers,
            resetSelectedTravelers = { selectedUserIds = emptySet() },
        )
    }
}

@Composable
private fun TravelerSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(text = stringResource(resource = Res.string.tavern_search_travelers_hint))
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun BarFolk(
    visibleTravelers: List<TravelerState>,
    travelerCount: Int,
    searchQuery: String,
    selectedUserIds: Set<String>,
    onToggleTraveler: (TravelerState) -> Unit,
    modifier: Modifier = Modifier,
) {
    // An active search that matches nobody, as opposed to an empty Tavern, gets its own message.
    val isSearchWithoutMatches = searchQuery.isNotEmpty() && visibleTravelers.isEmpty()

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(bottom = 24.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(resource = Res.string.tavern_travelers_to_challenge),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                AnimatedContent(
                    targetState = travelerCount,
                    label = TRAVELERS_IN_TAVERN_COUNT_LABEL,
                ) { count ->
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(height = 8.dp))

            Text(
                text = stringResource(resource = Res.string.tavern_challenge_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isSearchWithoutMatches) {
            item {
                Text(
                    text =
                        stringResource(resource = Res.string.tavern_search_no_matches, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                )
            }
        }

        items(items = visibleTravelers) { travelerState ->
            travelerState.ToCard(
                isSelected = travelerState.userId in selectedUserIds,
                onClick = { onToggleTraveler(travelerState) },
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
    AnimatedVisibility(
        visible = selectedTravelers.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
        exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }),
        label = CHALLENGE_BUTTON_VISIBILITY_LABEL,
        modifier = modifier,
    ) {
        val onSendChallengerNotifications =
            TavernActionsProvider.current.onSendChallengerNotifications

        AppButton(
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

@Composable
private fun TravelerState.ToCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selection is derived to exclude busy travelers, so a card is never both selected and busy.
    AnimatedContent(
        targetState = isSelected,
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = TRAVELER_CARD_SELECTION_LABEL,
    ) { selected ->
        if (selected) {
            SelectedTravelerCard(displayName = displayName, rating = rating, onClick = onClick)
        } else {
            UnselectedTravelerCard(
                displayName = displayName,
                rating = rating,
                isBusy = tavernStatus == TavernStatus.Busy,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun SelectedTravelerCard(
    displayName: String,
    rating: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.travelerCardSize(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        TravelerCardLabel(name = displayName, rating = rating, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UnselectedTravelerCard(
    displayName: String,
    rating: Long,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.travelerCardSize(),
        enabled = !isBusy,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                disabledContentColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
    ) {
        TravelerCardLabel(
            name = if (isBusy) "$displayName (Busy)" else displayName,
            rating = rating,
            fontWeight = FontWeight.Normal,
        )
    }
}

/** The shared footprint of a traveler card, so selected/unselected animate as the same size. */
private fun Modifier.travelerCardSize(): Modifier = fillMaxWidth().padding(horizontal = 20.dp)

/**
 * The shared centered label used by both the selected and unselected traveler cards: the name, with
 * the player's rating beneath it when they have one (computer opponents and unrated players show
 * just the name).
 */
@Composable
private fun TravelerCardLabel(
    name: String,
    rating: Long,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
        )

        if (rating > 0) {
            Text(
                text = stringResource(resource = Res.string.tavern_traveler_rating, rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TheBarPreview() {
    TheBar(
        userTraveler = exampleUserTraveler,
        otherTravelers = exampleOtherTravelers,
        modifier = Modifier.fillMaxSize(),
    )
}
