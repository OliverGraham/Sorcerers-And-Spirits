package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgress
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun MatchFinishedContent(
    finishedState: ArenaState.Finished,
    gauntletProgress: GauntletProgress,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val headline = if (gauntletProgress.isFinalMatch) "Gauntlet Finished" else "Match Finished"

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text =
                "Match ${gauntletProgress.currentMatch} of ${gauntletProgress.matchesPerGauntlet}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.padding(12.dp))

        WinnerBanner(winner = finishedState.winner)

        Spacer(modifier = Modifier.padding(12.dp))

        MatchRecap(players = finishedState.players)
    }
}

@Composable
private fun WinnerBanner(winner: PlayerState.Selected?, modifier: Modifier = Modifier) {
    val text =
        if (winner != null) {
            "${winner.displayName} wins as the ${winner.characterClass.className}!"
        } else {
            "No survivors — it's a draw"
        }

    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MatchRecap(players: List<PlayerState.Selected>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEach { player -> PlayerRecapRow(player = player) }
    }
}

@Composable
private fun PlayerRecapRow(player: PlayerState.Selected, modifier: Modifier = Modifier) {
    val isConscious = player.characterClass.isConscious

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isConscious) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = player.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = player.characterClass.className,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = if (isConscious) "Survived" else "Knocked out",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                    if (isConscious) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }
}

private fun exampleFinishedState(): ArenaState.Finished {
    val mage = CharacterClass.getMage().toState()
    val rogue = CharacterClass.getRogue().toState()

    return ArenaState.Finished(
        players =
            listOf(
                PlayerState.Selected(
                    userId = "player1",
                    displayName = "Sorcerer Supreme",
                    characterClass = mage,
                ),
                PlayerState.Selected(
                    userId = "player2",
                    displayName = "Spirit Walker",
                    characterClass = rogue.copy(hitPoints = 0),
                ),
            )
    )
}

@Preview
@Composable
private fun MatchFinishedContentPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedState(),
            gauntletProgress = GauntletProgress(currentMatch = 2),
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentFinalPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedState(),
            gauntletProgress = GauntletProgress(currentMatch = 3),
        )
    }
}
