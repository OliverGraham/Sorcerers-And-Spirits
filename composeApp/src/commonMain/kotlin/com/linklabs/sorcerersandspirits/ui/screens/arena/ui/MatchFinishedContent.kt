package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgress
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun MatchFinishedContent(
    gauntletProgress: GauntletProgress,
    modifier: Modifier = Modifier,
) {
    // TODO(SS-024) add something here if it makes sense or just keep the same ArenaState
    //  Maybe a display of the stats and recap of the match or something

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val headline = if (gauntletProgress.isFinalMatch) "Gauntlet Finished" else "Match Finished"

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Text(
            text =
                "Match ${gauntletProgress.currentMatch} of ${gauntletProgress.matchesPerGauntlet}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(gauntletProgress = GauntletProgress(currentMatch = 2))
    }
}

@Preview
@Composable
private fun MatchFinishedContentFinalPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(gauntletProgress = GauntletProgress(currentMatch = 3))
    }
}
