package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.classFamilyColors
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.flashCallout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val FLASH_HOLD_MILLIS = 850L
private const val FLASH_EXIT_MILLIS = 320L

/** Wall-clock time one queued flash callout occupies the overlay: its hold plus its pop-out. */
internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS
private val flashFontSize = 72.sp
private const val PI_FLOAT = 3.1415927f
private const val CONFETTI_COUNT = 24
private const val CONFETTI_MILLIS = 900
private const val CONFETTI_MIN_DISTANCE = 80f
private const val CONFETTI_MAX_DISTANCE = 220f
private const val CONFETTI_MIN_RADIUS = 4f
private const val CONFETTI_MAX_RADIUS = 9f

/**
 * The full-screen call-out for flash-worthy events (SS-148/SS-164): a big "Hit!"/"X Damage!"/
 * "Knocked Out!"/ability text that springs in with a bounce, holds, then pops out, over a one-shot
 * confetti burst in the four class colors. Driven by [flashMessages] with [flashCallout]:
 * newly-arrived messages are played one at a time, so a single resolution narrates as a sequence.
 * The history present on first composition is skipped so reconnecting never replays old call-outs.
 */
@Composable
internal fun FlashOverlay(flashMessages: List<ArenaMessageState>, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<FlashState?>(value = null) }
    val visibleState = remember { MutableTransitionState(initialState = false) }
    val latestMessages by rememberUpdatedState(newValue = flashMessages)

    LaunchedEffect(key1 = Unit) {
        var lastPlayedId = latestMessages.lastOrNull()?.id
        var lastSize = latestMessages.size
        snapshotFlow { latestMessages }
            .collect { messages ->
                val wasReset = messages.size < lastSize
                if (wasReset) lastPlayedId = null

                val fresh = messages.freshAfter(lastPlayedId = lastPlayedId)
                for (message in fresh) {
                    val callout = message.flashCallout ?: continue
                    current = FlashState(key = message.id, callout = callout)
                    visibleState.targetState = true
                    delay(timeMillis = FLASH_HOLD_MILLIS)
                    visibleState.targetState = false
                    delay(timeMillis = FLASH_EXIT_MILLIS)
                }
                lastPlayedId = messages.lastOrNull()?.id ?: lastPlayedId
                lastSize = messages.size
            }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val current = current ?: return@Box

        if (visibleState.targetState || visibleState.currentState) {
            ConfettiBurst(triggerKey = current.key, modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter =
                scaleIn(
                    initialScale = 0.3f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
        ) {
            FlashText(callout = current.callout)
        }
    }
}

@Composable
private fun FlashText(callout: FlashCallout, modifier: Modifier = Modifier) {
    val color = callout.actingClassType?.toColor() ?: Color.White
    Text(
        text = callout.text(),
        style =
            TextStyle(
                fontSize = flashFontSize,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center,
                shadow =
                    Shadow(color = Color.Black, offset = Offset(x = 0f, y = 4f), blurRadius = 8f),
            ),
        modifier = modifier,
    )
}

/** A one-shot burst: each particle flies out from the centre and fades, in a class-family color. */
@Composable
private fun ConfettiBurst(triggerKey: Any, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(initialValue = 0f) }
    val particles = remember(triggerKey) { confettiParticles() }

    LaunchedEffect(key1 = triggerKey) {
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = CONFETTI_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        val fade = (1f - t).coerceIn(minimumValue = 0f, maximumValue = 1f)
        particles.forEach { particle ->
            val travelPx = particle.distance.dp.toPx() * t
            val position =
                Offset(
                    x = center.x + cos(x = particle.angle) * travelPx,
                    y = center.y + sin(x = particle.angle) * travelPx,
                )
            drawCircle(
                color = particle.color.copy(alpha = fade),
                radius = particle.radius.dp.toPx() * (1f - 0.5f * t),
                center = position,
            )
        }
    }
}

/**
 * The flash messages that arrived after [lastPlayedId], i.e. the ones not yet played. A null
 * [lastPlayedId] means nothing has played, so every message is fresh; an unknown id (e.g. dropped
 * by the log cap) skips the batch rather than replaying the whole history.
 */
private fun List<ArenaMessageState>.freshAfter(lastPlayedId: String?): List<ArenaMessageState> {
    if (lastPlayedId == null) return this

    val lastPlayedIndex = indexOfLast { it.id == lastPlayedId }
    if (lastPlayedIndex == -1) return emptyList()

    return subList(lastPlayedIndex + 1, size)
}

private class FlashState(val key: String, val callout: FlashCallout)

private class ConfettiParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color,
)

private fun confettiParticles(): List<ConfettiParticle> {
    val colors = classFamilyColors()
    return List(size = CONFETTI_COUNT) { index ->
        ConfettiParticle(
            angle = Random.nextFloat() * 2f * PI_FLOAT,
            distance =
                CONFETTI_MIN_DISTANCE +
                    Random.nextFloat() * (CONFETTI_MAX_DISTANCE - CONFETTI_MIN_DISTANCE),
            radius =
                CONFETTI_MIN_RADIUS +
                    Random.nextFloat() * (CONFETTI_MAX_RADIUS - CONFETTI_MIN_RADIUS),
            color = colors[index % colors.size],
        )
    }
}
