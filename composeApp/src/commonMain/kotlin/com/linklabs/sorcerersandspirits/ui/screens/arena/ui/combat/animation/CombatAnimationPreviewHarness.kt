package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

// TODO: Remove "seat" terminology

internal const val PREVIEW_WIDTH_DP = 360
internal const val PREVIEW_HEIGHT_DP = 720
internal const val PREVIEW_ACTOR_ID = "actor"
internal const val PREVIEW_TARGET_ID = "target"

private const val PREVIEW_REPLAY_GAP_MILLIS = 700L
private const val MIN_STEP_HOLD_MILLIS = 400L
private const val AVATAR_MARKER_ALPHA = 0.24f

@Stable internal class PreviewSeats(val userIds: List<String>, val boundsFor: (String) -> Rect?)

@Composable
internal fun rememberSoloSeat(): PreviewSeats {
    val density = LocalDensity.current
    return remember(density) {
        val bounds = avatarBounds(density = density, centerYFraction = 0.5f)
        PreviewSeats(
            userIds = listOf(PREVIEW_ACTOR_ID),
            boundsFor = { userId -> if (userId == PREVIEW_ACTOR_ID) bounds else null },
        )
    }
}

@Composable
internal fun rememberDuoSeats(): PreviewSeats {
    val density = LocalDensity.current
    return remember(density) {
        val boundsByUserId =
            mapOf(
                PREVIEW_ACTOR_ID to avatarBounds(density = density, centerYFraction = 0.66f),
                PREVIEW_TARGET_ID to avatarBounds(density = density, centerYFraction = 0.2f),
            )
        PreviewSeats(userIds = boundsByUserId.keys.toList(), boundsFor = boundsByUserId::get)
    }
}

private fun avatarBounds(density: Density, centerYFraction: Float): Rect =
    with(density) {
        val width = 110.dp.toPx()
        val height = 150.dp.toPx()
        val centerX = PREVIEW_WIDTH_DP.dp.toPx() / 2f
        val centerY = PREVIEW_HEIGHT_DP.dp.toPx() * centerYFraction
        Rect(
            offset = Offset(x = centerX - width / 2f, y = centerY - height / 2f),
            size = Size(width = width, height = height),
        )
    }

// TODO: God, why so over-engineered?
@Composable
internal fun rememberReplayingCue(
    replayKey: Any,
    cueFor: (iteration: Int) -> ArenaMessageState,
): ArenaMessageState? =
    rememberReplayingCues(replayKey = replayKey) { iteration -> listOf(cueFor(iteration)) }

@Composable
internal fun rememberReplayingCues(
    replayKey: Any,
    cuesFor: (iteration: Int) -> List<ArenaMessageState>,
): ArenaMessageState? {
    var message by remember { mutableStateOf<ArenaMessageState?>(value = null) }

    LaunchedEffect(key1 = replayKey) {
        var iteration = 0
        while (true) {
            cuesFor(iteration++).forEach { step ->
                message = step
                delay(
                    timeMillis =
                        step
                            .playbackDurationMillis()
                            .coerceAtLeast(minimumValue = MIN_STEP_HOLD_MILLIS)
                )
            }
            delay(timeMillis = PREVIEW_REPLAY_GAP_MILLIS)
            message = null
            delay(timeMillis = PREVIEW_REPLAY_GAP_MILLIS)
        }
    }

    return message
}

@Composable
internal fun AbilityAnimationStage(
    accentClass: CharacterClassType,
    seats: PreviewSeats,
    overlay: @Composable () -> Unit,
) {
    val markerColor = accentClass.toColor()
    SorcerersAndSpiritsTheme {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .paint(
                        painter = painterResource(resource = Res.drawable.arena_background),
                        contentScale = ContentScale.Crop,
                    )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                seats.userIds.forEach { userId ->
                    val bounds = seats.boundsFor(userId) ?: return@forEach
                    drawRoundRect(
                        color = markerColor,
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                        cornerRadius = CornerRadius(x = bounds.width * 0.2f),
                        alpha = AVATAR_MARKER_ALPHA,
                    )
                }
            }
            overlay()
        }
    }
}
