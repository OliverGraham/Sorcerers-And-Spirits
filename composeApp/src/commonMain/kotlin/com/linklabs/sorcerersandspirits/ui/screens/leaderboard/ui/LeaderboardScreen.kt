package com.linklabs.sorcerersandspirits.ui.screens.leaderboard.ui

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

// TODO(SS-065): Placeholder LeaderboardScreen. Once stats are persisted (SS-021 + SS-072), show the
//  top players here with a route + ViewModel querying/ordering profile stats.
@Composable
internal fun LeaderboardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Leaderboard", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Top players will appear here once game stats are recorded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun LeaderboardScreenPreview() {
    SorcerersAndSpiritsTheme { LeaderboardScreen() }
}
