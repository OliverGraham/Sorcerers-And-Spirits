package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CharacterClassMessageInfoState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog.classHighlightedText
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_subtitle
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_duelist
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_gambler
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_mage
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_paladin
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tip_rogue
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_tips_label
import sorcerersandspirits.composeapp.generated.resources.arena_waiting_title

private const val TIP_ROTATION_MILLIS = 6000L
private const val TIP_CROSSFADE_MILLIS = 600

private const val DOT_PULSE_TRANSITION_LABEL = "WaitingDotsPulseTransition"
private const val DOT_PULSE_ALPHA_LABEL = "WaitingDotPulseAlpha"
private const val DOT_PULSE_DURATION_MILLIS = 600
private const val DOT_COUNT = 3
private const val DOT_MIN_ALPHA = 0.2f
private const val DOT_MAX_ALPHA = 1f

private val waitingTips: List<StringResource> =
    listOf(
        Res.string.arena_waiting_tip_doubles,
        Res.string.arena_waiting_tip_rogue,
        Res.string.arena_waiting_tip_mage,
        Res.string.arena_waiting_tip_duelist,
        Res.string.arena_waiting_tip_gambler,
        Res.string.arena_waiting_tip_paladin,
    )

private val tipClassColors: List<CharacterClassMessageInfoState> =
    CharacterClassType.entries
        .filter { type -> type != CharacterClassType.None }
        .map { type -> CharacterClassMessageInfoState(className = type.name, type = type) }

@Composable
internal fun ArenaWaitingForPlayers(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(resource = Res.string.arena_waiting_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        PulsingDots()

        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(resource = Res.string.arena_waiting_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(height = 32.dp))

        RotatingTipCard()
    }
}

@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
    val pulseTransition = rememberInfiniteTransition(label = DOT_PULSE_TRANSITION_LABEL)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        repeat(times = DOT_COUNT) { index ->
            val alpha by
                pulseTransition.animateFloat(
                    initialValue = DOT_MIN_ALPHA,
                    targetValue = DOT_MAX_ALPHA,
                    animationSpec =
                    infiniteRepeatable(
                        animation =
                        tween(
                            durationMillis = DOT_PULSE_DURATION_MILLIS,
                            delayMillis = index * DOT_PULSE_DURATION_MILLIS / DOT_COUNT,
                        ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = DOT_PULSE_ALPHA_LABEL,
                )

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(size = 10.dp).graphicsLayer { this.alpha = alpha },
            ) {}
        }
    }
}

@Composable
private fun RotatingTipCard(modifier: Modifier = Modifier) {
    var tipIndex by remember { mutableIntStateOf(value = 0) }

    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(timeMillis = TIP_ROTATION_MILLIS)
            tipIndex = (tipIndex + 1) % waitingTips.size
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(all = 20.dp)) {
            Text(
                text = stringResource(resource = Res.string.arena_waiting_tips_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            AnimatedContent(
                targetState = tipIndex,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(durationMillis = TIP_CROSSFADE_MILLIS),
                    ) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = TIP_CROSSFADE_MILLIS))
                },
            ) { index ->
                Text(
                    text =
                    classHighlightedText(
                        text = stringResource(resource = waitingTips[index]),
                        coloredClassNames = tipClassColors,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaWaitingForPlayersPreview() {
    SorcerersAndSpiritsTheme { ArenaWaitingForPlayers() }
}
