package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.CueEffectOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCues
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val RIPOSTE_APPEAR_MILLIS = 200
internal const val RIPOSTE_PARRY_MILLIS = 260
internal const val RIPOSTE_STAB_MILLIS = 200
private const val RIPOSTE_EMBED_MILLIS = 150L
private const val RIPOSTE_FADE_MILLIS = 220

private const val PARRY_DEFLECT_DEGREES = 20f
private const val PARRY_VIBRATION_DEGREES = 6f
private const val PARRY_VIBRATION_CYCLES = 5f
private const val WHIFF_OVERSHOOT_TRAVEL = 1.3f
private const val RIPOSTE_BURST_PEAK_SCALE = 1.2f
private const val UPRIGHT_DEGREES = -90f

/**
 * The paired blades stand upright, tips toward the top of the screen, in the waiting parry pose.
 */
private val upright = Offset(x = 0f, y = -1f)

/**
 * How long the blades take to rise into their ready pose, the whole of the Ready phase (SS-204).
 */
internal const val RIPOSTE_READY_MILLIS: Long = RIPOSTE_APPEAR_MILLIS.toLong()

/**
 * When the counter-stab lands, from the start of the resolve, so the attacker's flinch meets it.
 */
internal const val RIPOSTE_STRIKE_MILLIS: Long =
    (RIPOSTE_PARRY_MILLIS + RIPOSTE_STAB_MILLIS).toLong()

/** Wall-clock time of the resolve, so the message sequencer holds the roll message to the end. */
internal val RIPOSTE_TOTAL_MILLIS: Long =
    RIPOSTE_STRIKE_MILLIS + RIPOSTE_EMBED_MILLIS + RIPOSTE_FADE_MILLIS

/**
 * The animation channels of one Riposte (SS-204): [bladeAlpha] fades the paired blades in and out,
 * [parry] beats the forward blade aside in its quick deflecting flick, and [stab] swings the rear
 * blade down from upright into the counter-thrust — with the impact pair flaring the burst on a
 * landed reversal.
 */
@Stable
internal class RiposteEffectState {
    val bladeAlpha = Animatable(initialValue = 0f)
    val parry = Animatable(initialValue = 0f)
    val stab = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<RiposteScene?>(value = null)
}

/**
 * The fixed geometry of one Riposte, measured before it plays. Both blades stand upright and
 * side-by-side at the Duelist: the forward parry blade off [parryHand] out ahead toward the
 * attacker, the rear counter blade off [attackHand] alongside. The counter blade swings down [axis]
 * until its tip reaches [strikePoint] on the attacker.
 */
@Immutable
internal data class RiposteScene(
    val axis: Offset,
    val parryHand: Offset,
    val attackHand: Offset,
    val strikePoint: Offset,
    val bladeLengthPx: Float,
    val accentColor: Color,
)

/**
 * The Duelist's Riposte effect (SS-204). The moment the Duelist's armor is passed, the paired
 * blades rise upright and side-by-side into a waiting parry stance — the forward blade held out
 * ahead toward the attacker, the rear blade alongside — and hold there through the roll-off. Once
 * the roll resolves the forward blade flicks a quick deflecting beat, and on a landed counter the
 * rear blade swings down into a thrust across to the attacker with a class-color burst, or on a
 * failed one sails past and fades in flight. Anchored to the measured avatar bounds via
 * [avatarBoundsFor], and sits out any cue that is not a Riposte.
 */
@Composable
internal fun RiposteOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { RiposteEffectState() }
    val density = LocalDensity.current

    CueEffectOverlay(
        animationCue = animationCue,
        select = { it as? CombatAnimationCue.Riposte },
        onCancel = { state.scene = null },
        play = playLabel@{ cue ->
                val duelistBounds = avatarBoundsFor(cue.duelistUserId) ?: return@playLabel
                val attackerBounds = avatarBoundsFor(cue.attackerUserId) ?: return@playLabel
                val scene =
                    riposteScene(
                        duelistBounds = duelistBounds,
                        attackerBounds = attackerBounds,
                        accentColor = CharacterClassType.Duelist.toColor(),
                        density = density,
                    ) ?: return@playLabel
                state.play(scene = scene, phase = cue.phase)
            },
        modifier = modifier,
        draw = { drawRiposte(state = state) },
    )
}

private fun riposteScene(
    duelistBounds: Rect,
    attackerBounds: Rect,
    accentColor: Color,
    density: Density,
): RiposteScene? {
    val approach = attackerBounds.center - duelistBounds.center
    val approachDistance = approach.getDistance()
    if (approachDistance == 0f) return null

    val axis = approach / approachDistance
    val lateral = Offset(x = -axis.y, y = axis.x)
    val parryForwardPx = with(density) { 34.dp.toPx() }
    val attackForwardPx = with(density) { 10.dp.toPx() }
    val handSpreadPx = with(density) { 20.dp.toPx() }

    return RiposteScene(
        axis = axis,
        parryHand = duelistBounds.center + axis * parryForwardPx - lateral * handSpreadPx,
        attackHand = duelistBounds.center + axis * attackForwardPx + lateral * handSpreadPx,
        strikePoint = attackerBounds.center,
        bladeLengthPx = with(density) { rapierBladeLength.toPx() },
        accentColor = accentColor,
    )
}

private suspend fun RiposteEffectState.play(scene: RiposteScene, phase: RipostePhase) {
    when (phase) {
        RipostePhase.Ready -> raiseIntoGuard(scene = scene)
        RipostePhase.Landed -> resolve(scene = scene, isLanded = true)
        RipostePhase.Missed -> resolve(scene = scene, isLanded = false)
    }
}

/** The parry stance rises the moment the Duelist is struck and holds, waiting out the roll-off. */
private suspend fun RiposteEffectState.raiseIntoGuard(scene: RiposteScene) {
    parry.snapTo(targetValue = 0f)
    stab.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    bladeAlpha.snapTo(targetValue = 0f)
    this.scene = scene

    bladeAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = RIPOSTE_APPEAR_MILLIS),
    )
}

/** The roll resolves: the forward blade beats, then the rear blade counters home or sails past. */
private suspend fun RiposteEffectState.resolve(scene: RiposteScene, isLanded: Boolean) {
    this.scene = scene
    bladeAlpha.snapTo(targetValue = 1f)
    parry.snapTo(targetValue = 0f)
    stab.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)

    parry.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = RIPOSTE_PARRY_MILLIS, easing = FastOutSlowInEasing),
    )

    if (isLanded) {
        land()
    } else {
        sailPast()
    }

    this.scene = null
}

/**
 * A landed counter: the rear blade stabs home, the burst flares on the attacker, then both fade.
 */
private suspend fun RiposteEffectState.land() {
    stab.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = RIPOSTE_STAB_MILLIS, easing = FastOutLinearInEasing),
    )
    coroutineScope {
        launch {
            impactBurst(
                impactAlpha = impactAlpha,
                impactScale = impactScale,
                peakScale = RIPOSTE_BURST_PEAK_SCALE,
            )
        }
        launch {
            delay(timeMillis = RIPOSTE_EMBED_MILLIS)
            bladeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = RIPOSTE_FADE_MILLIS),
            )
        }
    }
}

/**
 * A failed counter: the rear blade thrusts but overshoots past the attacker and fades in flight.
 */
private suspend fun RiposteEffectState.sailPast() {
    coroutineScope {
        launch {
            stab.animateTo(
                targetValue = WHIFF_OVERSHOOT_TRAVEL,
                animationSpec =
                    tween(
                        durationMillis = RIPOSTE_STAB_MILLIS + RIPOSTE_FADE_MILLIS,
                        easing = FastOutLinearInEasing,
                    ),
            )
        }
        launch {
            delay(timeMillis = RIPOSTE_STAB_MILLIS.toLong())
            bladeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = RIPOSTE_FADE_MILLIS),
            )
        }
    }
}

private fun DrawScope.drawRiposte(state: RiposteEffectState) {
    val scene = state.scene ?: return

    drawParryBlade(scene = scene, state = state)
    drawCounterBlade(scene = scene, state = state)

    val isBursting = state.impactAlpha.value > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(
            center = scene.strikePoint,
            scale = state.impactScale.value,
            alpha = state.impactAlpha.value,
            accentColor = scene.accentColor,
        )
    }
}

/**
 * The forward blade: it stands upright ahead of the Duelist, then flicks a quick economical beat
 * about its own guard — a small deflection that bats the incoming attack aside with a fine settling
 * tremor before returning to upright, on both a landed and a failed counter.
 */
private fun DrawScope.drawParryBlade(scene: RiposteScene, state: RiposteEffectState) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val progress = state.parry.value
    val deflectDegrees = sin(x = (progress * PI).toDouble()).toFloat() * PARRY_DEFLECT_DEGREES
    val vibrationDegrees =
        sin(x = (progress * PI * PARRY_VIBRATION_CYCLES).toDouble()).toFloat() *
            PARRY_VIBRATION_DEGREES *
            (1f - progress)
    val tipDirection = upright.rotatedBy(degrees = deflectDegrees + vibrationDegrees)
    drawBlade(hand = scene.parryHand, tipDirection = tipDirection, scene = scene, alpha = alpha)
}

/**
 * The rear blade: it waits upright alongside the parry blade, then swings down from upright onto
 * the attack line and rides the stab until its tip reaches the attacker on a landed counter, or
 * overshoots past on a failed one.
 */
private fun DrawScope.drawCounterBlade(scene: RiposteScene, state: RiposteEffectState) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val travel = state.stab.value
    val swingFraction = travel.coerceAtMost(maximumValue = 1f)
    val axisDegrees = atan2(y = scene.axis.y, x = scene.axis.x) * 180f / PI.toFloat()
    val degrees =
        lerpAngle(fromDegrees = UPRIGHT_DEGREES, toDegrees = axisDegrees, fraction = swingFraction)
    val radians = degrees * PI.toFloat() / 180f
    val tipDirection = Offset(x = cos(x = radians), y = sin(x = radians))

    val landedHand = scene.strikePoint - scene.axis * scene.bladeLengthPx
    val hand = lerp(start = scene.attackHand, stop = landedHand, fraction = travel)
    drawBlade(hand = hand, tipDirection = tipDirection, scene = scene, alpha = alpha)
}

/** One blade drawn from its [hand] guard along [tipDirection], its tip leading. */
private fun DrawScope.drawBlade(
    hand: Offset,
    tipDirection: Offset,
    scene: RiposteScene,
    alpha: Float,
) {
    val tip = hand + tipDirection * scene.bladeLengthPx
    val bladeDegrees = atan2(y = tipDirection.y, x = tipDirection.x) * 180f / PI.toFloat() - 90f
    rotate(degrees = bladeDegrees, pivot = tip) {
        translate(left = tip.x, top = tip.y) {
            drawSwordShape(accentColor = scene.accentColor, alpha = alpha)
        }
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteLandedPreview() {
    DuelistRipostePreview(resolution = RipostePhase.Landed)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteMissedPreview() {
    DuelistRipostePreview(resolution = RipostePhase.Missed)
}

@Composable
private fun DuelistRipostePreview(resolution: RipostePhase) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCues(replayKey = resolution) { iteration ->
            listOf(
                CombatAnimationCue.Riposte(
                    id = "riposte-ready-$iteration",
                    duelistUserId = PREVIEW_ACTOR_ID,
                    attackerUserId = PREVIEW_TARGET_ID,
                    phase = RipostePhase.Ready,
                ),
                CombatAnimationCue.Riposte(
                    id = "riposte-resolve-$iteration",
                    duelistUserId = PREVIEW_ACTOR_ID,
                    attackerUserId = PREVIEW_TARGET_ID,
                    phase = resolution,
                ),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Duelist, seats = seats) {
        RiposteOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
