package com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.ErrorContent
import com.linklabs.sorcerersandspirits.ui.common.LoadingIndicator
import com.linklabs.sorcerersandspirits.ui.common.MenuTopBar
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.AbilityRulesState
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ClassRulesState
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.GameRulesScreenViewModel
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.GameRulesState
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.preview.exampleGameRulesState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GameRulesScreen(
    viewModel: GameRulesScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()

    Scaffold(modifier = modifier, topBar = { MenuTopBar(title = "Game Rules") }) { paddingValues ->
        GameRulesScreen(
            uiState = uiState,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(paddingValues = paddingValues),
        )
    }
}

@Composable
private fun GameRulesScreen(
    uiState: UiState<GameRulesState>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Error -> ErrorContent(onRetry = onRetry, modifier = modifier)
        is UiState.Loaded -> GameRulesContent(state = uiState.data, modifier = modifier)
        is UiState.Loading -> LoadingIndicator(modifier = modifier)
    }
}

@Composable
private fun GameRulesContent(state: GameRulesState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        item { RulesSection(title = "How Combat Works", paragraphs = state.combatRules) }

        item { RulesSection(title = "Game Terms", paragraphs = state.gameTerms) }

        item {
            Text(
                text = "Classes & Abilities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        items(items = state.classes, key = { it.className }) { classRules ->
            ClassRulesCard(classRules = classRules)
        }
    }
}

@Composable
private fun RulesSection(title: String, paragraphs: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ClassRulesCard(classRules: ClassRulesState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                text = classRules.className,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                "Health ${classRules.hitPoints}  ·  " +
                    "Armor ${classRules.armorClass}  ·  " +
                    "Speed ${classRules.speed}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            AbilityEntry(kind = "Offensive", ability = classRules.offensiveAbility)
            AbilityEntry(kind = "Defensive", ability = classRules.defensiveAbility)
        }
    }
}

@Composable
private fun AbilityEntry(kind: String, ability: AbilityRulesState, modifier: Modifier = Modifier) {
    if (ability.description.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$kind · ${ability.name}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = ability.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun GameRulesScreenPreview() {
    SorcerersAndSpiritsTheme {
        GameRulesScreen(uiState = UiState.Loaded(data = exampleGameRulesState), onRetry = {})
    }
}
