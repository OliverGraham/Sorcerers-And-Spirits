package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CueEffectOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val PREPARE_APPEAR_MILLIS = 160
internal const val PREPARE_TWIRL_MILLIS = 450
internal const val PREPARE_PULL_BACK_MILLIS = 140
internal const val PREPARE_GLEAM_MILLIS = 340
internal const val PREPARE_THRUST_MILLIS = 190
private const val PREPARE_EMBED_MILLIS = 170L
private const val PREPARE_FADE_MILLIS = 220

private const val TWIRL_DEGREES = 360f
private const val WHIFF_OVERSHOOT_TRAVEL = 1.18f

/** When the thrust launches: the blades have appeared, twirled, pulled back, and gleamed. */
internal val PREPARE_ATTACK_WINDUP_MILLIS: Long =
    (PREPARE_APPEAR_MILLIS + PREPARE_TWIRL_MILLIS + PREPARE_PULL_BACK_MILLIS + PREPARE_GLEAM_MILLIS)
        .toLong()

/** When the blades land, so the shared defender reaction (SS-191) meets the thrust. */
internal val PREPARE_ATTACK_STRIKE_MILLIS: Long =
    PREPARE_ATTACK_WINDUP_MILLIS + PREPARE_THRUST_MILLIS

/** Wall-clock time of the whole effect, so the message sequencer holds its message to the end. */
internal val PREPARE_ATTACK_TOTAL_MILLIS: Long =
    PREPARE_ATTACK_STRIKE_MILLIS + PREPARE_EMBED_MILLIS + PREPARE_FADE_MILLIS

/**
 * The animation channels of one Prepare Attack (SS-203): [bladeAlpha] fades the paired rapiers in
 * and out, [windup] twirls each a full revolution around the Duelist's hands, [pullBack] draws them
 * back a touch, [gleam] sweeps the glint down each blade, and [thrust] carries them into the target
 * — with the impact pair flaring the knockout burst on a landed strike.
 */
@Stable
internal class PrepareAttackEffectState {
    val bladeAlpha = Animatable(initialValue = 0f)
    val windup = Animatable(initialValue = 0f)
    val pullBack = Animatable(initialValue = 0f)
    val gleam = Animatable(initialValue = 0f)
    val thrust = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<PrepareAttackScene?>(value = null)
}

/**
 * The fixed geometry of one Prepare Attack, measured before it plays. The blades hang off
 * [handAnchor] spread [handSpreadPx] to either side along [perpAxis], and thrust down [axis] until
 * their tips converge on [strikePoint].
 */
@Immutable
internal data class PrepareAttackScene(
    val axis: Offset,
    val handAnchor: Offset,
    val strikePoint: Offset,
    val handSpreadPx: Float,
    val convergencePx: Float,
    val pullBackPx: Float,
    val bladeLengthPx: Float,
    val accentColor: Color,
) {
    val perpAxis = Offset(x = -axis.y, y = axis.x)
}

/**
 * The Duelist's Prepare Attack effect (SS-203): the paired rapiers appear together at the Duelist's
 * sides aimed at the target, twirl a showy full revolution around the hands — each blade sweeping
 * backwards over its own side and coming around to where it started — pull back a touch, flash a
 * gleam down each blade, and thrust forward as one: converging on the defender with a knockout
 * burst on a successful roll, or sailing past the sidestepping defender and fading in flight on a
 * whiff. Anchored to the measured avatar bounds via [avatarBoundsFor], and sits out any cue that is
 * not a Prepare Attack.
 */
@Composable
internal fun PrepareAttackOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { PrepareAttackEffectState() }
    val density = LocalDensity.current

    CueEffectOverlay(
        animationCue = animationCue,
        select = { it as? CombatAnimationCue.PrepareAttack },
        onCancel = { state.scene = null },
        play = playLabel@{ cue ->
                val duelistBounds = avatarBoundsFor(cue.duelistUserId) ?: return@playLabel
                val targetBounds = avatarBoundsFor(cue.targetUserId) ?: return@playLabel
                val scene =
                    prepareAttackScene(
                        duelistBounds = duelistBounds,
                        targetBounds = targetBounds,
                        accentColor = CharacterClassType.Duelist.toColor(),
                        density = density,
                    ) ?: return@playLabel
                state.play(scene = scene, isSuccessful = cue.isSuccessful)
            },
        modifier = modifier,
        draw = { drawPrepareAttack(state = state) },
    )
}

private fun prepareAttackScene(
    duelistBounds: Rect,
    targetBounds: Rect,
    accentColor: Color,
    density: Density,
): PrepareAttackScene? {
    val approach = targetBounds.center - duelistBounds.center
    val approachDistance = approach.getDistance()
    if (approachDistance == 0f) return null

    return PrepareAttackScene(
        axis = approach / approachDistance,
        handAnchor = duelistBounds.center,
        strikePoint = targetBounds.center,
        handSpreadPx = with(density) { 38.dp.toPx() },
        convergencePx = with(density) { 10.dp.toPx() },
        pullBackPx = with(density) { 12.dp.toPx() },
        bladeLengthPx = with(density) { rapierBladeLength.toPx() },
        accentColor = accentColor,
    )
}

private suspend fun PrepareAttackEffectState.play(
    scene: PrepareAttackScene,
    isSuccessful: Boolean,
) {
    bladeAlpha.snapTo(targetValue = 0f)
    windup.snapTo(targetValue = 0f)
    pullBack.snapTo(targetValue = 0f)
    gleam.snapTo(targetValue = 0f)
    thrust.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = scene

    bladeAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PREPARE_APPEAR_MILLIS),
    )
    windup.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PREPARE_TWIRL_MILLIS, easing = FastOutSlowInEasing),
    )
    pullBack.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PREPARE_PULL_BACK_MILLIS),
    )
    gleam.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PREPARE_GLEAM_MILLIS, easing = LinearEasing),
    )
    thrustForward()

    if (isSuccessful) {
        land()
    } else {
        sailPast()
    }

    this.scene = null
}

/** The release: the twirl has already brought the tips back onto the target, so the blades fly. */
private suspend fun PrepareAttackEffectState.thrustForward() {
    thrust.animateTo(
        targetValue = 1f,
        animationSpec =
            tween(durationMillis = PREPARE_THRUST_MILLIS, easing = FastOutLinearInEasing),
    )
}

/** A landed strike: the blades sit embedded while the knockout burst flares, then fade out. */
private suspend fun PrepareAttackEffectState.land() {
    coroutineScope {
        launch { impactBurst() }
        launch {
            delay(timeMillis = PREPARE_EMBED_MILLIS)
            bladeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PREPARE_FADE_MILLIS),
            )
        }
    }
}

/** The whiff: the thrust carries past the sidestepping target and the blades fade in flight. */
private suspend fun PrepareAttackEffectState.sailPast() {
    coroutineScope {
        launch {
            thrust.animateTo(
                targetValue = WHIFF_OVERSHOOT_TRAVEL,
                animationSpec = tween(durationMillis = PREPARE_FADE_MILLIS),
            )
        }
        launch {
            bladeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PREPARE_FADE_MILLIS),
            )
        }
    }
}

private suspend fun PrepareAttackEffectState.impactBurst() {
    impactAlpha.snapTo(targetValue = 1f)
    impactScale.snapTo(targetValue = 0f)
    coroutineScope {
        launch {
            impactScale.animateTo(targetValue = 1.3f, animationSpec = tween(durationMillis = 160))
        }
        launch {
            delay(timeMillis = 60L)
            impactAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 240))
        }
    }
}

private fun DrawScope.drawPrepareAttack(state: PrepareAttackEffectState) {
    val scene = state.scene ?: return

    drawPreparedBlade(scene = scene, state = state, side = -1f)
    drawPreparedBlade(scene = scene, state = state, side = 1f)

    val isBursting = state.impactAlpha.value > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawKnockoutBurst(
            scene = scene,
            scale = state.impactScale.value,
            alpha = state.impactAlpha.value,
        )
    }
}

/**
 * One prepared rapier: it hangs off the hand on its [side] of the Duelist, spins a full revolution
 * about that hand during the windup — mirrored, so each blade sweeps backwards over its own side —
 * and rides the thrust until its tip converges on the strike point.
 */
private fun DrawScope.drawPreparedBlade(
    scene: PrepareAttackScene,
    state: PrepareAttackEffectState,
    side: Float,
) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val restingHand = scene.handAnchor + scene.perpAxis * (side * scene.handSpreadPx)
    val pulledHand = restingHand - scene.axis * (state.pullBack.value * scene.pullBackPx)
    val landedHand =
        scene.strikePoint + scene.perpAxis * (side * scene.convergencePx) -
            scene.axis * scene.bladeLengthPx
    val hand = lerp(start = pulledHand, stop = landedHand, fraction = state.thrust.value)

    val twirlDegrees = side * TWIRL_DEGREES * state.windup.value
    val tipDirection = scene.axis.rotatedBy(degrees = twirlDegrees)
    val tip = hand + tipDirection * scene.bladeLengthPx
    val bladeDegrees = atan2(y = tipDirection.y, x = tipDirection.x) * 180f / PI.toFloat() - 90f

    rotate(degrees = bladeDegrees, pivot = tip) {
        translate(left = tip.x, top = tip.y) {
            drawSwordShape(accentColor = scene.accentColor, alpha = alpha)
            drawBladeGleam(
                progress = state.gleam.value,
                bladeLengthPx = scene.bladeLengthPx,
                alpha = alpha,
            )
        }
    }
}

/**
 * The shiny gleam: a white glint that sweeps hilt-to-tip along the cocked blade, brightest at the
 * middle of its run and gone at either end so it reads as light flashing across the steel.
 */
private fun DrawScope.drawBladeGleam(progress: Float, bladeLengthPx: Float, alpha: Float) {
    val shimmer = sin(x = progress.toDouble() * PI).toFloat()
    if (shimmer <= 0f) return

    val glintCenterY = -bladeLengthPx * (1f - progress)
    val glintHalfLength = bladeLengthPx * 0.14f
    val start = Offset(x = 0f, y = (glintCenterY - glintHalfLength).coerceAtLeast(-bladeLengthPx))
    val end = Offset(x = 0f, y = (glintCenterY + glintHalfLength).coerceAtMost(0f))

    drawLine(
        color = Color.White,
        start = start,
        end = end,
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * shimmer * 0.45f,
    )
    drawLine(
        color = Color.White,
        start = start,
        end = end,
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * shimmer,
    )
    drawCircle(
        color = Color.White,
        radius = 3.dp.toPx(),
        center = Offset(x = 0f, y = glintCenterY),
        alpha = alpha * shimmer,
    )
}

/** The knockout flare: a white-cored burst rimmed in the Duelist's color at the strike point. */
private fun DrawScope.drawKnockoutBurst(scene: PrepareAttackScene, scale: Float, alpha: Float) {
    val radius = 40.dp.toPx() * scale
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(Color.White, scene.accentColor.copy(alpha = 0.8f), Color.Transparent),
                center = scene.strikePoint,
                radius = radius,
            ),
        radius = radius,
        center = scene.strikePoint,
        alpha = alpha,
    )
}

internal fun Offset.rotatedBy(degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(
        x = x * cos(x = radians) - y * sin(x = radians),
        y = x * sin(x = radians) + y * cos(x = radians),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistPrepareAttackSuccessPreview() {
    DuelistPrepareAttackPreview(isSuccessful = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistPrepareAttackWhiffPreview() {
    DuelistPrepareAttackPreview(isSuccessful = false)
}

@Composable
private fun DuelistPrepareAttackPreview(isSuccessful: Boolean) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = isSuccessful) { iteration ->
            CombatAnimationCue.PrepareAttack(
                id = "prepare-attack-$iteration",
                duelistUserId = PREVIEW_ACTOR_ID,
                targetUserId = PREVIEW_TARGET_ID,
                isSuccessful = isSuccessful,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Duelist, seats = seats) {
        PrepareAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
