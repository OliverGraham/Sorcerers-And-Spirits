package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.TeleportPhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.GestureClassAvatar
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CueEffectOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloSeat
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.rememberAvatarGesture
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_background

private const val PORTAL_OPEN_MILLIS = 420
private const val SWIRL_PERIOD_MILLIS = 2600
private const val SWALLOW_MILLIS = 300
private const val PORTAL_CLOSE_MILLIS = 300
private const val PORTAL_SNAP_MILLIS = 200
private const val GRAY_BLOOM_MILLIS = 260
private const val GRAY_FADE_MILLIS = 460

/** How many revolutions each swirl strand winds from the void out to the rim. */
private const val SPIRAL_TURNS = 1.35f

/**
 * How big the portal opens, off the avatar width, and how far it floats above the head, off its own
 * height.
 */
private const val PORTAL_RADIUS_X_FRACTION = 0.5f
private const val PORTAL_RADIUS_Y_FRACTION = 0.6f
private const val PORTAL_LIFT_FRACTION = 0.55f

private const val GRAY_WASH_PEAK_ALPHA = 0.55f

private val PORTAL_CORE = Color(color = 0xFFF2FBFF)
private val PORTAL_RIM = Color(color = 0xFF8FE0FF)
private val PORTAL_GLOW = Color(color = 0xFF3AA0FF)
private val PORTAL_SPIRAL = Color(color = 0xFF6FC8FF)
private val PORTAL_VOID = Color(color = 0xFF081733)
private val PORTAL_GRAY = Color(color = 0xFF9AA0A6)

/**
 * The state of one Teleport portal (SS-211). The choreography drives these Animatables and the draw
 * layer reads them, so the portal churns and closes without recomposing anything. [open] grows the
 * portal from nothing to full; [swirl] winds monotonically so the spiral reads as one endless turn
 * even across the roll-off holds; [swallow] flashes bright as the escaping Mage passes through; and
 * [gray] blooms the failure wash over the scene.
 */
@Stable
internal class TeleportPortalState {
    val open = Animatable(initialValue = 0f)
    val swirl = Animatable(initialValue = 0f)
    val swallow = Animatable(initialValue = 0f)
    val gray = Animatable(initialValue = 0f)

    var scene by mutableStateOf<TeleportScene?>(value = null)
}

/** The fixed geometry of one portal: an upright ellipse centered on [center]. */
@Immutable
internal data class TeleportScene(val center: Offset, val radiusX: Float, val radiusY: Float)

/**
 * The Mage's Teleport portal (SS-211). The instant the Mage's armor is passed a swirling blue
 * portal tears open above them and churns endlessly through the roll-off. On the resolving roll it
 * either flashes and closes behind the escaping Mage, or — on a failed escape — snaps shut too soon
 * and washes the scene gray. Anchored to the Mage's measured seat via [avatarBoundsFor]; sits out
 * any cue that is not a Teleport.
 */
@Composable
internal fun MageTeleportOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { TeleportPortalState() }

    CueEffectOverlay(
        animationCue = animationCue,
        select = { it as? CombatAnimationCue.Teleport },
        onCancel = { state.scene = null },
        play = playLabel@{ cue ->
                val mageBounds = avatarBoundsFor(cue.mageUserId) ?: return@playLabel
                state.play(scene = teleportScene(mageBounds = mageBounds), cue = cue)
            },
        modifier = modifier,
        draw = { drawTeleport(state = state) },
    )
}

/**
 * How long each Teleport phase's portal runs, so the message sequencer holds its callout until the
 * portal has opened, closed behind the escape, or snapped shut and washed gray — rather than
 * cutting it off. The hold phase re-asserts the already-open portal, so it adds no wait.
 */
internal fun teleportOverlayDurationMillis(phase: TeleportPhase, didEscape: Boolean?): Long =
    when (phase) {
        TeleportPhase.Open -> PORTAL_OPEN_MILLIS.toLong()

        TeleportPhase.Hold -> 0L

        TeleportPhase.Resolve ->
            if (didEscape == true) {
                (SWALLOW_MILLIS + PORTAL_CLOSE_MILLIS).toLong()
            } else {
                maxOf(PORTAL_SNAP_MILLIS, GRAY_BLOOM_MILLIS).toLong() + GRAY_FADE_MILLIS
            }
    }

private fun teleportScene(mageBounds: Rect): TeleportScene {
    val radiusX = mageBounds.width * PORTAL_RADIUS_X_FRACTION
    val radiusY = mageBounds.width * PORTAL_RADIUS_Y_FRACTION
    val liftPx = radiusY * PORTAL_LIFT_FRACTION
    val center = Offset(x = mageBounds.center.x, y = mageBounds.top - liftPx)
    return TeleportScene(center = center, radiusX = radiusX, radiusY = radiusY)
}

private suspend fun TeleportPortalState.play(
    scene: TeleportScene,
    cue: CombatAnimationCue.Teleport,
) {
    when (cue.phase) {
        TeleportPhase.Open -> open(scene = scene)

        TeleportPhase.Hold -> hold(scene = scene)

        TeleportPhase.Resolve ->
            if (cue.didEscape == true) resolveEscape(scene = scene) else resolveFail(scene = scene)
    }
}

/** The portal tears open the moment the Mage's armor is passed and starts its endless churn. */
private suspend fun TeleportPortalState.open(scene: TeleportScene) {
    this.scene = scene
    open.snapTo(targetValue = 0f)
    swirl.snapTo(targetValue = 0f)
    swallow.snapTo(targetValue = 0f)
    gray.snapTo(targetValue = 0f)

    open.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PORTAL_OPEN_MILLIS, easing = FastOutSlowInEasing),
    )
    swirlForever()
}

/**
 * Each roll-off die re-asserts the already-open portal and resumes its churn without re-opening.
 */
private suspend fun TeleportPortalState.hold(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)
    swirlForever()
}

/**
 * The endless swirl: winds one full turn at a time from wherever it left off, so a fresh roll-off
 * die resumes the rotation seamlessly rather than snapping the spiral back to its start.
 */
private suspend fun TeleportPortalState.swirlForever() {
    while (true) {
        swirl.animateTo(
            targetValue = swirl.value + 1f,
            animationSpec = tween(durationMillis = SWIRL_PERIOD_MILLIS, easing = LinearEasing),
        )
    }
}

/**
 * A successful escape: the portal flashes as the Mage passes through, then spins shut behind them.
 */
private suspend fun TeleportPortalState.resolveEscape(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)

    coroutineScope {
        launch {
            swallow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SWALLOW_MILLIS),
            )
            swallow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PORTAL_CLOSE_MILLIS),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = swirl.value + 1.2f,
                animationSpec =
                    tween(
                        durationMillis = SWALLOW_MILLIS + PORTAL_CLOSE_MILLIS,
                        easing = LinearEasing,
                    ),
            )
        }
        launch {
            delay(timeMillis = SWALLOW_MILLIS.toLong())
            open.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = PORTAL_CLOSE_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }

    this.scene = null
}

/** A failed escape: the portal snaps shut too soon while a gray wash blooms over the scene. */
private suspend fun TeleportPortalState.resolveFail(scene: TeleportScene) {
    this.scene = scene
    if (open.value == 0f) open.snapTo(targetValue = 1f)

    coroutineScope {
        launch {
            open.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = PORTAL_SNAP_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            swirl.animateTo(
                targetValue = swirl.value + 0.6f,
                animationSpec = tween(durationMillis = PORTAL_SNAP_MILLIS, easing = LinearEasing),
            )
        }
        launch {
            gray.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GRAY_BLOOM_MILLIS),
            )
        }
    }
    gray.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = GRAY_FADE_MILLIS))

    this.scene = null
}

private fun DrawScope.drawTeleport(state: TeleportPortalState) {
    val scene = state.scene ?: return

    val open = state.open.value
    if (open > 0f) {
        drawPortal(
            scene = scene,
            open = open,
            swirl = state.swirl.value,
            swallow = state.swallow.value,
        )
    }

    val gray = state.gray.value
    if (gray > 0f) {
        drawRect(color = PORTAL_GRAY.copy(alpha = gray * GRAY_WASH_PEAK_ALPHA), size = size)
    }
}

/**
 * The portal drawn as an upright ellipse: an outer blue glow, a bright rim, a deep receding void
 * for depth, three winding spiral strands, and a handful of sparks orbiting the mouth — with a
 * white flash blooming from the center as an escaping Mage passes through.
 */
private fun DrawScope.drawPortal(scene: TeleportScene, open: Float, swirl: Float, swallow: Float) {
    val center = scene.center
    val radius = scene.radiusX * open
    val yScale = scene.radiusY / scene.radiusX

    scale(scaleX = 1f, scaleY = yScale, pivot = center) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(PORTAL_GLOW.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = radius * 1.7f,
                ),
            radius = radius * 1.7f,
            center = center,
        )
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(PORTAL_VOID, PORTAL_GLOW.copy(alpha = 0.85f)),
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
        for (strand in 0 until 3) {
            drawSpiralStrand(
                center = center,
                radius = radius,
                swirl = swirl + strand / 3f,
                alpha = 0.6f * open,
            )
        }
        drawCircle(
            color = PORTAL_RIM.copy(alpha = open),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.12f),
        )
        for (spark in 0 until 5) {
            val angle = (swirl * 2f + spark / 5f) * 2f * PI.toFloat()
            val orbit = radius * 0.72f
            drawCircle(
                color = PORTAL_CORE.copy(alpha = 0.85f * open),
                radius = radius * 0.05f,
                center = center + Offset(x = cos(angle) * orbit, y = sin(angle) * orbit),
            )
        }
        if (swallow > 0f) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(PORTAL_CORE.copy(alpha = swallow), Color.Transparent),
                        center = center,
                        radius = radius * (0.4f + 0.6f * swallow),
                    ),
                radius = radius * (0.4f + 0.6f * swallow),
                center = center,
            )
        }
    }
}

/** One winding spiral strand from the void out to the rim, rotated by [swirl]. */
private fun DrawScope.drawSpiralStrand(center: Offset, radius: Float, swirl: Float, alpha: Float) {
    val steps = 56
    val path = Path()
    for (step in 0..steps) {
        val fraction = step / steps.toFloat()
        val angle = (fraction * SPIRAL_TURNS + swirl) * 2f * PI.toFloat()
        val strandRadius = fraction * radius
        val point = center + Offset(x = cos(angle) * strandRadius, y = sin(angle) * strandRadius)
        if (step == 0) {
            path.moveTo(x = point.x, y = point.y)
        } else {
            path.lineTo(x = point.x, y = point.y)
        }
    }
    drawPath(
        path = path,
        color = PORTAL_SPIRAL.copy(alpha = alpha),
        style = Stroke(width = radius * 0.05f),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageTeleportEscapePreview() {
    MageTeleportPreview(didEscape = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MageTeleportFailPreview() {
    MageTeleportPreview(didEscape = false)
}

/**
 * The whole Teleport played over a real seated Mage: the portal opens and churns through two roll
 * dice, then the Mage either rises through it and vanishes, or lunges and is thrown back as the
 * scene grays — the seat gesture and the portal overlay driven off the one replaying cue.
 */
@Composable
private fun MageTeleportPreview(didEscape: Boolean) {
    val seats = rememberSoloSeat()
    val bounds = seats.boundsFor(PREVIEW_ACTOR_ID)
    val density = LocalDensity.current
    val classState = previewCharacterClassState(type = CharacterClassType.Mage)
    val cue =
        rememberReplayingCues(replayKey = didEscape) { iteration ->
            listOf(
                teleportPreviewCue(
                    iteration = iteration,
                    phase = TeleportPhase.Open,
                    didEscape = null,
                ),
                teleportPreviewCue(
                    iteration = iteration,
                    phase = TeleportPhase.Hold,
                    didEscape = null,
                ),
                teleportPreviewCue(
                    iteration = iteration,
                    phase = TeleportPhase.Hold,
                    didEscape = null,
                ),
                teleportPreviewCue(
                    iteration = iteration,
                    phase = TeleportPhase.Resolve,
                    didEscape = didEscape,
                ),
            )
        }
    val gestureState =
        rememberAvatarGesture(animationCue = cue, userId = PREVIEW_ACTOR_ID, isTopSeat = false)

    SorcerersAndSpiritsTheme {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .paint(
                        painter = painterResource(resource = Res.drawable.arena_background),
                        contentScale = ContentScale.Crop,
                    )
        ) {
            if (bounds != null) {
                GestureClassAvatar(
                    characterClass = classState,
                    gestureState = gestureState,
                    modifier =
                        Modifier.offset(
                                x = with(density) { bounds.left.toDp() },
                                y = with(density) { bounds.top.toDp() },
                            )
                            .size(
                                width = with(density) { bounds.width.toDp() },
                                height = with(density) { bounds.height.toDp() },
                            ),
                )
            }
            MageTeleportOverlay(
                animationCue = cue,
                avatarBoundsFor = seats.boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun teleportPreviewCue(
    iteration: Int,
    phase: TeleportPhase,
    didEscape: Boolean?,
): CombatAnimationCue.Teleport =
    CombatAnimationCue.Teleport(
        id = "teleport-$phase-$didEscape-$iteration",
        mageUserId = PREVIEW_ACTOR_ID,
        phase = phase,
        didEscape = didEscape,
    )
