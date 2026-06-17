package com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO(SS-067): Placeholder GameRulesScreen. Populate with the game rules and terminology
//  (Turn/Round/Match/Gauntlet, combat rules, class abilities) behind a route + ViewModel.
@Composable
internal fun GameRulesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Game Rules", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Game rules and class abilities will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun GameRulesScreenPreview() {
    SorcerersAndSpiritsTheme { GameRulesScreen() }
}
