package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.fadedBlackBackground
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
import sorcerersandspirits.composeapp.generated.resources.arena_message_next_match_starting_soon
import kotlin.math.roundToInt

private const val BOUNCE_SPEED_DP_PER_SECOND = 110f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val BOUNCE_COLOR_TRANSITION_LABEL = "NextMatchBounceColor"
private const val BOUNCE_COLOR_SHIFT_LABEL = "NextMatchBounceColorShift"
private const val BOUNCE_COLOR_CYCLE_MILLIS = 2000

@Composable
internal fun ArenaMatchState.Finished.MatchFinishedContent(
    screenState: ArenaScreenState,
    timerState: () -> ArenaTimerState,
    modifier: Modifier = Modifier,
) {
    val gauntletProgressState = screenState.gauntletProgressState
    var isLogExpanded by remember { mutableStateOf(value = false) }

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
                latestMessage = screenState.latestFinishedMessage,
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

        if (timerState().isVisible) {
            NextMatchBounceOverlay()
        }

        ExpandingMessageLog(
            isExpanded = isLogExpanded,
            messageGroups = screenState.messageGroups,
            onDismiss = { isLogExpanded = false },
            isCombatOngoing = false,
        )
    }
}

@Composable
private fun NextMatchBounceOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    val colorTransition = rememberInfiniteTransition(label = BOUNCE_COLOR_TRANSITION_LABEL)
    val sweepProgress by
        colorTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
            infiniteRepeatable(
                animation =
                tween(durationMillis = BOUNCE_COLOR_CYCLE_MILLIS, easing = LinearEasing),
            ),
            label = BOUNCE_COLOR_SHIFT_LABEL,
        )

    val sweepColors = remember { classFamilyColors().let { it + it.first() } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(receiver = density) { maxWidth.toPx() }
        val containerHeightPx = with(receiver = density) { maxHeight.toPx() }
        val speedPxPerSecond = with(receiver = density) { BOUNCE_SPEED_DP_PER_SECOND.dp.toPx() }

        var contentSize by remember { mutableStateOf(value = IntSize.Zero) }
        var position by remember { mutableStateOf(value = Offset.Zero) }
        var velocity by
            remember { mutableStateOf(value = Offset(x = speedPxPerSecond, y = speedPxPerSecond)) }

        LaunchedEffect(key1 = contentSize, key2 = containerWidthPx, key3 = containerHeightPx) {
            val hasBounds =
                contentSize != IntSize.Zero && containerWidthPx > 0f && containerHeightPx > 0f
            if (!hasBounds) return@LaunchedEffect

            val maxX = (containerWidthPx - contentSize.width).coerceAtLeast(minimumValue = 0f)
            val maxY = (containerHeightPx - contentSize.height).coerceAtLeast(minimumValue = 0f)

            var lastFrameNanos = 0L
            while (true) {
                val frameNanos = withFrameNanos { it }
                val isFirstFrame = lastFrameNanos == 0L
                if (isFirstFrame) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaSeconds = (frameNanos - lastFrameNanos) / NANOS_PER_SECOND
                lastFrameNanos = frameNanos

                var nextX = position.x + velocity.x * deltaSeconds
                var nextY = position.y + velocity.y * deltaSeconds
                var nextVelocity = velocity

                val hasHitHorizontalEdge = nextX <= 0f || nextX >= maxX
                if (hasHitHorizontalEdge) {
                    nextX = nextX.coerceIn(minimumValue = 0f, maximumValue = maxX)
                    nextVelocity = nextVelocity.copy(x = -nextVelocity.x)
                }

                val hasHitVerticalEdge = nextY <= 0f || nextY >= maxY
                if (hasHitVerticalEdge) {
                    nextY = nextY.coerceIn(minimumValue = 0f, maximumValue = maxY)
                    nextVelocity = nextVelocity.copy(y = -nextVelocity.y)
                }

                position = Offset(x = nextX, y = nextY)
                velocity = nextVelocity
            }
        }

        val textWidthPx = contentSize.width.toFloat().coerceAtLeast(minimumValue = 1f)
        val sweepStartX = sweepProgress * textWidthPx
        val sweepBrush =
            Brush.linearGradient(
                colors = sweepColors,
                start = Offset(x = sweepStartX, y = 0f),
                end = Offset(x = sweepStartX + textWidthPx, y = 0f),
                tileMode = TileMode.Repeated,
            )

        Text(
            text = stringResource(resource = Res.string.arena_message_next_match_starting_soon),
            style =
            TextStyle(brush = sweepBrush, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier =
            Modifier.offset {
                IntOffset(x = position.x.roundToInt(), y = position.y.roundToInt())
            }
                .onSizeChanged { contentSize = it }
                .fadedBlackBackground(alpha = 0.5f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
