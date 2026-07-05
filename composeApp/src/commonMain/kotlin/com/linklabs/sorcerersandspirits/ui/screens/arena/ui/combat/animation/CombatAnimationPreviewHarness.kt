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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.playbackDurationMillis
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

/**
 * Shared scaffolding for the isolated interactive previews each combat effect overlay hosts
 * (SS-192…SS-212). An overlay drives itself off a [CombatAnimationCue] and a lambda giving the
 * acting avatar's bounds, so a preview stands one overlay alone against fixed seats — no arena,
 * view model, or Firebase behind it. The cue is replayed on a loop so the effect keeps firing while
 * the preview runs in Android Studio's Interactive Mode; a static preview frame does not animate.
 *
 * Each overlay's own file holds its previews; this file holds only what they all reuse — the stage,
 * the seats the effects aim at, and the replay driver.
 */
internal const val PREVIEW_WIDTH_DP = 360

internal const val PREVIEW_HEIGHT_DP = 720

/** The avatar whose ability is being previewed — the striker, gambler, or duelist. */
internal const val PREVIEW_ACTOR_ID = "actor"

/** The other avatar an effect reaches — the defender, target, or countered attacker. */
internal const val PREVIEW_TARGET_ID = "target"

private const val PREVIEW_REPLAY_GAP_MILLIS = 700L

private const val MIN_STEP_HOLD_MILLIS = 400L

private const val AVATAR_MARKER_ALPHA = 0.24f

/** The seats an ability preview aims at, plus the ids to mark on the stage. */
@Stable internal class PreviewSeats(val userIds: List<String>, val boundsFor: (String) -> Rect?)

/**
 * A single acting avatar planted center stage, for abilities that involve only their own caster.
 */
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

/**
 * The actor low and the target high, facing off, so a weapon or counter travels between them the
 * way it does across the real combat stage.
 */
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

/**
 * Drives one cue on a loop so its effect keeps replaying in Interactive Mode, holding for the cue's
 * own animation length before clearing and firing the next round.
 */
@Composable
internal fun rememberReplayingCue(
    replayKey: Any,
    cueFor: (iteration: Int) -> CombatAnimationCue,
): CombatAnimationCue? =
    rememberReplayingCues(replayKey = replayKey) { iteration -> listOf(cueFor(iteration)) }

/**
 * Drives a sequence of cues on a loop, holding each for its own animation length before the next,
 * so a multi-phase effect like a Riposte plays its guard-then-resolve arc in full. Starts null so
 * an overlay, which skips a cue already live at first composition, plays the opening cue.
 */
@Composable
internal fun rememberReplayingCues(
    replayKey: Any,
    cuesFor: (iteration: Int) -> List<CombatAnimationCue>,
): CombatAnimationCue? {
    var cue by remember { mutableStateOf<CombatAnimationCue?>(value = null) }
    LaunchedEffect(key1 = replayKey) {
        var iteration = 0
        while (true) {
            cuesFor(iteration++).forEach { step ->
                cue = step
                delay(
                    timeMillis =
                        step
                            .playbackDurationMillis()
                            .coerceAtLeast(minimumValue = MIN_STEP_HOLD_MILLIS)
                )
            }
            delay(timeMillis = PREVIEW_REPLAY_GAP_MILLIS)
            cue = null
            delay(timeMillis = PREVIEW_REPLAY_GAP_MILLIS)
        }
    }
    return cue
}

/**
 * The shared preview backdrop: the arena background with a translucent silhouette at each seat, so
 * the origin an effect launches from and the target it reaches both read even without the real
 * avatars, and the hosted [overlay] painted on top.
 */
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
