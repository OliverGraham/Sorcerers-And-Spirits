package com.linklabs.sorcerersandspirits.ui.screens.achievements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.achievement.descriptionResource
import com.linklabs.sorcerersandspirits.ui.common.achievement.displayNameResource
import com.linklabs.sorcerersandspirits.ui.screens.achievements.AchievementRowState
import com.linklabs.sorcerersandspirits.ui.screens.achievements.AchievementsScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.achievements.AchievementsState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.achievements_locked
import sorcerersandspirits.composeapp.generated.resources.achievements_progress
import sorcerersandspirits.composeapp.generated.resources.achievements_title
import sorcerersandspirits.composeapp.generated.resources.achievements_unlocked

@Composable
internal fun AchievementsScreen(
    viewModel: AchievementsScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()

    AchievementsScreen(uiState = uiState, modifier = modifier)
}

@Composable
private fun AchievementsScreen(uiState: UiState<AchievementsState>, modifier: Modifier = Modifier) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = {}, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
        is UiState.Loaded -> AchievementsContent(state = uiState.data, modifier = modifier)
    }
}

@Composable
private fun AchievementsContent(state: AchievementsState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                Text(
                    text = stringResource(resource = Res.string.achievements_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text =
                    stringResource(
                        resource = Res.string.achievements_progress,
                        state.unlockedCount,
                        state.totalCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(items = state.achievements, key = { row -> row.id.name }) { row ->
            AchievementRow(row = row)
        }
    }
}

@Composable
private fun AchievementRow(row: AchievementRowState, modifier: Modifier = Modifier) {
    val containerColor =
        if (row.isUnlocked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            AchievementStatusLabel(isUnlocked = row.isUnlocked)

            Text(
                text = stringResource(resource = row.id.displayNameResource),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(resource = row.id.descriptionResource),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AchievementStatusLabel(isUnlocked: Boolean, modifier: Modifier = Modifier) {
    val statusText =
        if (isUnlocked) {
            stringResource(resource = Res.string.achievements_unlocked)
        } else {
            stringResource(resource = Res.string.achievements_locked)
        }
    val statusColor =
        if (isUnlocked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Text(
        text = statusText,
        style = MaterialTheme.typography.labelMedium,
        color = statusColor,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AchievementsScreenPreview() {
    SorcerersAndSpiritsTheme {
        AchievementsScreen(
            uiState =
            UiState.Loaded(
                data =
                AchievementsState(
                    achievements =
                    listOf(
                        AchievementRowState(
                            id = AchievementId.WinAMatch,
                            isUnlocked = true,
                        ),
                        AchievementRowState(
                            id = AchievementId.WinAGauntlet,
                            isUnlocked = true,
                        ),
                        AchievementRowState(
                            id = AchievementId.WinAMatchWithEveryClass,
                            isUnlocked = false,
                        ),
                        AchievementRowState(
                            id = AchievementId.WinAGauntletWithEveryClass,
                            isUnlocked = false,
                        ),
                    ),
                    unlockedCount = 2,
                    totalCount = 4,
                ),
            ),
        )
    }
}
