package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.AnimatedSeconds
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgressState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_finished_countdown_seconds
import sorcerersandspirits.composeapp.generated.resources.arena_finished_draw
import sorcerersandspirits.composeapp.generated.resources.arena_finished_everyone_left
import sorcerersandspirits.composeapp.generated.resources.arena_finished_leave
import sorcerersandspirits.composeapp.generated.resources.arena_finished_match_progress
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_knocked_out
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_not_ready
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_ready
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_survived
import sorcerersandspirits.composeapp.generated.resources.arena_finished_waiting_for_others
import sorcerersandspirits.composeapp.generated.resources.arena_finished_winner

private const val READINESS_LABEL = "ReadinessLabel"
private const val READINESS_FADE_DURATION_MILLIS = 220

@Composable
internal fun MatchFinishedContent(
    finishedState: ArenaMatchState.Finished,
    gauntletProgressState: GauntletProgressState,
    hasReadiedForNextMatch: Boolean,
    secondsRemaining: () -> Int,
    messageGroups: List<ArenaMessageGroupState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(resource = gauntletProgressState.stringResources.titleResource),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text =
            stringResource(
                resource = Res.string.arena_finished_match_progress,
                gauntletProgressState.currentMatch,
                gauntletProgressState.matchesPerGauntlet,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.padding(12.dp))

        WinnerBanner(winner = finishedState.winner)

        Spacer(modifier = Modifier.padding(12.dp))

        MatchRecap(players = finishedState.players)

        Spacer(modifier = Modifier.padding(12.dp))

        FinishedMatchLog(messageGroups = messageGroups)

        Spacer(modifier = Modifier.padding(12.dp))

        MatchDecisionControls(
            canStartNextMatch = finishedState.canStartNextMatch,
            gauntletProgressState = gauntletProgressState,
            hasReadiedForNextMatch = hasReadiedForNextMatch,
            secondsRemaining = secondsRemaining,
        )
    }
}

@Composable
private fun MatchDecisionControls(
    canStartNextMatch: Boolean,
    gauntletProgressState: GauntletProgressState,
    hasReadiedForNextMatch: Boolean,
    secondsRemaining: () -> Int,
    modifier: Modifier = Modifier,
) {
    val actions = ArenaActionsProvider.current

    if (!canStartNextMatch) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(resource = Res.string.arena_finished_everyone_left),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AppButton(
                onClick = actions.onConfirmLeaveArena,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Text(text = stringResource(resource = Res.string.arena_finished_leave))
            }
        }
        return
    }

    val currentSeconds = secondsRemaining()
    val isCountdownRunning = currentSeconds > 0

    if (hasReadiedForNextMatch) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(resource = Res.string.arena_finished_waiting_for_others),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (isCountdownRunning) {
                CountdownText(
                    prefix =
                    stringResource(resource = gauntletProgressState.stringResources.countdownWaitingPrefixResource),
                    secondsRemaining = currentSeconds,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isCountdownRunning) {
            CountdownText(
                prefix = stringResource(resource = gauntletProgressState.stringResources.countdownStartPrefixResource),
                secondsRemaining = currentSeconds,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                onClick = actions.onConfirmLeaveArena,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Text(text = stringResource(resource = Res.string.arena_finished_leave))
            }
            AppButton(onClick = actions.onStayForAnotherMatch) {
                Text(text = stringResource(resource = gauntletProgressState.stringResources.continueActionResource))
            }
        }
    }
}

@Composable
private fun CountdownText(prefix: String, secondsRemaining: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        AnimatedSeconds(seconds = secondsRemaining) { seconds ->
            Text(
                text =
                stringResource(resource = Res.string.arena_finished_countdown_seconds, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun WinnerBanner(winner: PlayerState.Selected?, modifier: Modifier = Modifier) {
    val text =
        if (winner != null) {
            stringResource(
                resource = Res.string.arena_finished_winner,
                winner.displayName,
                winner.characterClass.className,
            )
        } else {
            stringResource(resource = Res.string.arena_finished_draw)
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
            },
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text =
                    if (isConscious) {
                        stringResource(resource = Res.string.arena_finished_player_survived)
                    } else {
                        stringResource(resource = Res.string.arena_finished_player_knocked_out)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color =
                    if (isConscious) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                ReadinessLabel(isReadyForNextMatch = player.isReadyForNextMatch)
            }
        }
    }
}

@Composable
private fun ReadinessLabel(isReadyForNextMatch: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = isReadyForNextMatch,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = READINESS_FADE_DURATION_MILLIS)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = READINESS_FADE_DURATION_MILLIS))
        },
        label = READINESS_LABEL,
    ) { isReady ->
        Text(
            text =
            if (isReady) {
                stringResource(resource = Res.string.arena_finished_player_ready)
            } else {
                stringResource(resource = Res.string.arena_finished_player_not_ready)
            },
            style = MaterialTheme.typography.labelMedium,
            color =
            if (isReady) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun exampleMessageGroups(): List<ArenaMessageGroupState> {
    val mageInfo = CharacterClassMessageInfoState("Mage", CharacterClassType.Mage)
    val rogueInfo = CharacterClassMessageInfoState("Rogue", CharacterClassType.Rogue)

    return listOf(
        ArenaMessageGroupState.Setup(
            messages =
            listOf(
                ArenaMessageState.ConnectedToArena(id = "1", displayName = "Sorcerer Supreme"),
                ArenaMessageState.ChoseCharacterClass(
                    id = "2",
                    displayName = "Sorcerer Supreme",
                    characterClassInfo = mageInfo,
                ),
                ArenaMessageState.ChoseCharacterClass(
                    id = "3",
                    displayName = "Spirit Walker",
                    characterClassInfo = rogueInfo,
                ),
            ),
        ),
        ArenaMessageGroupState.Round(
            roundNumber = 1,
            messages =
            listOf(
                ArenaMessageState.NewRound(id = "4", roundNumber = 1),
                ArenaMessageState.NewTurn(id = "5", characterClassInfo = mageInfo),
                ArenaMessageState.NewTurn(id = "6", characterClassInfo = rogueInfo),
            ),
        ),
    )
}

private fun exampleFinishedState(): ArenaMatchState.Finished {
    val mage = previewCharacterClassState(type = CharacterClassType.Mage)
    val rogue = previewCharacterClassState(type = CharacterClassType.Rogue)

    return ArenaMatchState.Finished(
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
        ),
    )
}

@Preview
@Composable
private fun MatchFinishedContentPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedState(),
            gauntletProgressState = GauntletProgressState(currentMatch = 2),
            hasReadiedForNextMatch = false,
            secondsRemaining = { 0 },
            messageGroups = exampleMessageGroups(),
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentFinalPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedState(),
            gauntletProgressState = GauntletProgressState(currentMatch = 3),
            hasReadiedForNextMatch = false,
            secondsRemaining = { 0 },
            messageGroups = exampleMessageGroups(),
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentCountdownPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedStateWithReadiedPlayer(),
            gauntletProgressState = GauntletProgressState(currentMatch = 2),
            hasReadiedForNextMatch = false,
            secondsRemaining = { 45 },
            messageGroups = exampleMessageGroups(),
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentWaitingPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState = exampleFinishedStateWithReadiedPlayer(),
            gauntletProgressState = GauntletProgressState(currentMatch = 2),
            hasReadiedForNextMatch = true,
            secondsRemaining = { 45 },
            messageGroups = exampleMessageGroups(),
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentLoneSurvivorPreview() {
    SorcerersAndSpiritsTheme {
        MatchFinishedContent(
            finishedState =
            exampleFinishedState().copy(players = exampleFinishedState().players.take(1)),
            gauntletProgressState = GauntletProgressState(currentMatch = 2),
            hasReadiedForNextMatch = false,
            secondsRemaining = { 0 },
            messageGroups = exampleMessageGroups(),
        )
    }
}

private fun exampleFinishedStateWithReadiedPlayer(): ArenaMatchState.Finished {
    val baseState = exampleFinishedState()
    val readiedPlayers =
        baseState.players.mapIndexed { index, player ->
            player.copy(isReadyForNextMatch = index == 0)
        }

    return baseState.copy(players = readiedPlayers)
}
