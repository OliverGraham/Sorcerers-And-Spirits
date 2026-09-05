package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaScreenState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaTimerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.GauntletProgressState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.GestureClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.ExpandingMessageLog
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.MessagesBarHeaderTarget
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_finished_draw
import sorcerersandspirits.composeapp.generated.resources.arena_finished_everyone_left
import sorcerersandspirits.composeapp.generated.resources.arena_finished_leave
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_knocked_out
import sorcerersandspirits.composeapp.generated.resources.arena_finished_player_survived
import sorcerersandspirits.composeapp.generated.resources.arena_finished_waiting_for_others
import sorcerersandspirits.composeapp.generated.resources.arena_finished_winner

@Composable
internal fun ArenaMatchState.Finished.MatchFinishedContent(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    modifier: Modifier = Modifier,
) {
    val gauntletProgressState = screenState.gauntletProgressState
    var isLogExpanded by remember { mutableStateOf(value = false) }

    val latestMessage =
        if (timerState().isVisible) {
            ArenaMessageState.NextMatchStartingSoon
        } else {
            screenState.latestFinishedMessage
        }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MessagesBar(
                screenState = screenState,
                header =
                MessagesBarHeaderTarget.Finished(
                    titleResource = gauntletProgressState.stringResources.titleResource,
                    currentMatch = gauntletProgressState.currentMatch,
                    matchesPerGauntlet = gauntletProgressState.matchesPerGauntlet,
                ),
                latestMessage = latestMessage,
                timerState = timerState,
                isExpanded = isLogExpanded,
                onExpandedChange = { isLogExpanded = it },
                modifier = Modifier.windowInsetsPadding(insets = WindowInsets.statusBars),
            )

            Column(
                modifier = Modifier.weight(weight = 1f).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MatchRecap(players = players)

                Spacer(modifier = Modifier.padding(12.dp))

                WinnerBanner(winner = winner)

                Spacer(modifier = Modifier.padding(8.dp))

                if (winner != null) {
                    WinnerAvatar(
                        winner = winner,
                        modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
                    )
                }
            }

            MatchDecisionControls(
                canStartNextMatch = canStartNextMatch,
                gauntletProgressState = gauntletProgressState,
                hasReadiedForNextMatch = screenState.hasReadiedForNextMatch,
                modifier =
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(insets = WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }

        ExpandingMessageLog(
            isExpanded = isLogExpanded,
            messageGroups = screenState.messageGroups,
            onDismiss = { isLogExpanded = false },
        )
    }
}

@Composable
private fun WinnerAvatar(winner: PlayerState.Selected, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        GestureClassAvatar(
            animatingMessage = null,
            isTopSeat = false,
            userId = winner.userId,
            characterClass = winner.characterClass,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MatchDecisionControls(
    canStartNextMatch: Boolean,
    gauntletProgressState: GauntletProgressState,
    hasReadiedForNextMatch: Boolean,
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
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
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
            }
        }
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

private fun exampleFinishedStateWithReadiedPlayer(): ArenaMatchState.Finished {
    val baseState = exampleFinishedState()
    val readiedPlayers =
        baseState.players.mapIndexed { index, player ->
            player.copy(isReadyForNextMatch = index == 0)
        }

    return baseState.copy(players = readiedPlayers)
}

private fun previewFinishedScreenState(
    finishedState: ArenaMatchState.Finished,
    currentMatch: Int,
    hasReadiedForNextMatch: Boolean,
): ArenaScreenState = ArenaScreenState(
    showForfeitDialog = false,
    isUserPlayerKnockedOut = false,
    hasReadiedForNextMatch = hasReadiedForNextMatch,
    arenaMatchState = finishedState,
    messageGroups = exampleMessageGroups(),
    gauntletProgressState = GauntletProgressState(currentMatch = currentMatch),
)

@Preview
@Composable
private fun MatchFinishedContentPreview() {
    SorcerersAndSpiritsTheme {
        val finishedState = exampleFinishedState()
        finishedState.MatchFinishedContent(
            screenState =
            previewFinishedScreenState(
                finishedState = finishedState,
                currentMatch = 2,
                hasReadiedForNextMatch = false,
            ),
            timerState = { ArenaTimerState(secondsRemaining = 0) },
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentFinalPreview() {
    SorcerersAndSpiritsTheme {
        val finishedState = exampleFinishedState()
        finishedState.MatchFinishedContent(
            screenState =
            previewFinishedScreenState(
                finishedState = finishedState,
                currentMatch = 3,
                hasReadiedForNextMatch = false,
            ),
            timerState = { ArenaTimerState(secondsRemaining = 0) },
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentCountdownPreview() {
    SorcerersAndSpiritsTheme {
        val finishedState = exampleFinishedStateWithReadiedPlayer()
        finishedState.MatchFinishedContent(
            screenState =
            previewFinishedScreenState(
                finishedState = finishedState,
                currentMatch = 2,
                hasReadiedForNextMatch = false,
            ),
            timerState = { ArenaTimerState(secondsRemaining = 10) },
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentWaitingPreview() {
    SorcerersAndSpiritsTheme {
        val finishedState = exampleFinishedStateWithReadiedPlayer()
        finishedState.MatchFinishedContent(
            screenState =
            previewFinishedScreenState(
                finishedState = finishedState,
                currentMatch = 2,
                hasReadiedForNextMatch = true,
            ),
            timerState = { ArenaTimerState(secondsRemaining = 10) },
        )
    }
}

@Preview
@Composable
private fun MatchFinishedContentLoneSurvivorPreview() {
    SorcerersAndSpiritsTheme {
        val finishedState = exampleFinishedState().let { it.copy(players = it.players.take(1)) }
        finishedState.MatchFinishedContent(
            screenState =
            previewFinishedScreenState(
                finishedState = finishedState,
                currentMatch = 2,
                hasReadiedForNextMatch = false,
            ),
            timerState = { ArenaTimerState(secondsRemaining = 0) },
        )
    }
}
