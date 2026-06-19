package com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.LeaderboardRowState
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.LeaderboardScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.leaderboard.LeaderboardState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.leaderboard_empty
import sorcerersandspirits.composeapp.generated.resources.leaderboard_gauntlet_wins
import sorcerersandspirits.composeapp.generated.resources.leaderboard_match_wins
import sorcerersandspirits.composeapp.generated.resources.leaderboard_rank
import sorcerersandspirits.composeapp.generated.resources.leaderboard_subtitle
import sorcerersandspirits.composeapp.generated.resources.leaderboard_title
import sorcerersandspirits.composeapp.generated.resources.leaderboard_you

@Composable
internal fun LeaderboardScreen(
    viewModel: LeaderboardScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()

    LeaderboardScreen(uiState = uiState, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
private fun LeaderboardScreen(
    uiState: UiState<LeaderboardState>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
        is UiState.Loaded -> LeaderboardContent(state = uiState.data, modifier = modifier)
    }
}

@Composable
private fun LeaderboardContent(state: LeaderboardState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                Text(
                    text = stringResource(resource = Res.string.leaderboard_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(resource = Res.string.leaderboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.entries.isEmpty()) {
            item { EmptyLeaderboard() }
        } else {
            items(items = state.entries, key = { it.rank }) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardRowState, modifier: Modifier = Modifier) {
    val containerColor =
        if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Text(
                text = stringResource(resource = Res.string.leaderboard_rank, entry.rank),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp),
            )

            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (entry.isCurrentUser) {
                        Text(
                            text = stringResource(resource = Res.string.leaderboard_you),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text =
                        stringResource(
                            resource = Res.string.leaderboard_match_wins,
                            entry.matchesWon,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text =
                    stringResource(
                        resource = Res.string.leaderboard_gauntlet_wins,
                        entry.gauntletsWon,
                    ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyLeaderboard(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(resource = Res.string.leaderboard_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
    )
}

@Preview
@Composable
private fun LeaderboardScreenPreview() {
    SorcerersAndSpiritsTheme {
        LeaderboardScreen(
            uiState =
                UiState.Loaded(
                    data =
                        LeaderboardState(
                            entries =
                                listOf(
                                    LeaderboardRowState(
                                        rank = 1,
                                        displayName = "Gandalf",
                                        gauntletsWon = 12,
                                        matchesWon = 41,
                                        isCurrentUser = false,
                                    ),
                                    LeaderboardRowState(
                                        rank = 2,
                                        displayName = "Radagast",
                                        gauntletsWon = 9,
                                        matchesWon = 33,
                                        isCurrentUser = true,
                                    ),
                                    LeaderboardRowState(
                                        rank = 3,
                                        displayName = "Saruman",
                                        gauntletsWon = 7,
                                        matchesWon = 28,
                                        isCurrentUser = false,
                                    ),
                                )
                        )
                ),
            onRetry = {},
        )
    }
}
