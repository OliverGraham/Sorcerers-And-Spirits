package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.gestureFor
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BARD_WAVE_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_APPEAR_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_STRIKE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_WINDUP_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_GLEAM_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_PULL_BACK_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_THRUST_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_TWIRL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_STRIKE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_RAISE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_READY_HOLD_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SLASH_STAGGER_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.TWIN_SWORD_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.ALL_IN_SETTLE_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.ALL_IN_TOSS_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.THROW_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.meleeDashDistance
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NECROMANCER_REAP_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_DASH_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_EMBED_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.MELEE_THRUST_MILLIS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REACTION_DELAY_MILLIS = 170L
private const val SHAKE_SEGMENT_MILLIS = 55
private const val SLASH_LEAN_MILLIS = 130
private const val SLASH_RECOVER_MILLIS = 240

/** Total wall-clock time of [allInToss]'s three phases; kept in step with their tween durations. */
private const val ALL_IN_TOSS_GESTURE_MILLIS = 620
private const val COLLAPSE_ROTATION_DEGREES = 80f
private const val COLLAPSE_FEET_SLIDE_FRACTION = 0.4f
private const val HURT_TINT_MAX_RED_OFFSET = 130f
private const val HURT_TINT_CHANNEL_DAMP = 0.45f
private const val ARMOR_TINT_MAX_BLUE_OFFSET = 110f
private const val ARMOR_TINT_CHANNEL_DAMP = 0.3f

/**
 * Wall-clock length of each seated-avatar gesture's own body — the animation after any injected
 * reaction delay — so the sequencer can hold a message until the bodies come to rest (SS-201). Kept
 * in step with the choreography in [play], the same way the weapon overlays report their own runs.
 */
private const val FLINCH_BODY_MILLIS = 380L
private const val FLINCH_HEAVY_BODY_MILLIS = 470L
private const val SIDESTEP_BODY_MILLIS = 520L
private const val COLLAPSE_BODY_MILLIS = 650L
private const val DODGE_FLOURISH_BODY_MILLIS = 450L
private const val DODGE_BRACE_BODY_MILLIS = 860L
private const val REARM_BODY_MILLIS = 650L
private const val CAST_BODY_MILLIS = 480L
private const val ALL_IN_BUST_JOLT_MILLIS = 160L

private val feetPivot = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)

/**
 * The transforms one seated avatar animates through while acting out a gesture (SS-191). The
 * choreography drives these Animatables; the avatar art reads them in its graphics layer and color
 * filter, so playback never recomposes anything but the art itself. A [Collapse][AvatarGesture]
 * deliberately never resets: the downed, drained avatar is the resting state a knocked-out player
 * holds through the match-ending finish hold (SS-184).
 */
@Stable
internal class AvatarGestureState {
    val offsetX = Animatable(initialValue = 0f)
    val offsetY = Animatable(initialValue = 0f)
    val rotation = Animatable(initialValue = 0f)
    val scaleX = Animatable(initialValue = 1f)
    val scaleY = Animatable(initialValue = 1f)
    val collapseFraction = Animatable(initialValue = 0f)
    val hurtTint = Animatable(initialValue = 0f)
    val armorTint = Animatable(initialValue = 0f)
    val saturation = Animatable(initialValue = 1f)

    val colorFilter: ColorFilter?
        get() {
            val isAtRest = hurtTint.value == 0f && armorTint.value == 0f && saturation.value == 1f
            if (isAtRest) return null

            val matrix = ColorMatrix().apply { setToSaturation(sat = saturation.value) }
            matrix *= hurtTintMatrix(tint = hurtTint.value)
            matrix *= armorTintMatrix(tint = armorTint.value)
            return ColorFilter.colorMatrix(colorMatrix = matrix)
        }
}

/**
 * The seated avatar art with its gesture transforms applied, pivoting at the feet so lunges read as
 * a push off the ground and a collapse tips over from the ankles. Kept as its own composable so the
 * per-frame color-filter reads recompose only the art.
 */
@Composable
internal fun GestureClassAvatar(
    characterClass: CharacterClassState,
    gestureState: AvatarGestureState,
    modifier: Modifier = Modifier,
) {
    ClassAvatar(
        characterClass = characterClass,
        colorFilter = gestureState.colorFilter,
        modifier = modifier.graphicsLayer { applyGesture(gestureState = gestureState) },
    )
}

/**
 * The collapse pose is computed here rather than in the choreography because only the layer knows
 * the seat's size: a body tipped a full [COLLAPSE_ROTATION_DEGREES] about the feet sweeps out its
 * own height sideways, which pushed half the avatar off screen. Instead the downed body shrinks
 * toward the width the seat can hold and its feet slide toward the seat edge, so the avatar lies
 * down inside its own seat and stays fully visible through the finish hold (SS-184).
 */
private fun GraphicsLayerScope.applyGesture(gestureState: AvatarGestureState) {
    val collapse = gestureState.collapseFraction.value
    val lyingScale = (size.width / size.height).coerceAtMost(maximumValue = 1f)
    val collapseScale = 1f + (lyingScale - 1f) * collapse

    transformOrigin = feetPivot
    translationX = gestureState.offsetX.value - size.width * COLLAPSE_FEET_SLIDE_FRACTION * collapse
    translationY = gestureState.offsetY.value
    rotationZ = gestureState.rotation.value + COLLAPSE_ROTATION_DEGREES * collapse
    scaleX = gestureState.scaleX.value * collapseScale
    scaleY = gestureState.scaleY.value * collapseScale
}

/**
 * Plays this seat's role in each newly-arrived [animationCue]: the cue resolves the player's
 * gesture, the top seat strikes downward and the bottom seat upward, and a cue already present on
 * first composition is skipped so a seating change never replays an old gesture.
 */
@Composable
internal fun rememberAvatarGesture(
    animationCue: CombatAnimationCue?,
    userId: String,
    isTopSeat: Boolean,
): AvatarGestureState {
    val gestureState = remember { AvatarGestureState() }
    val density = LocalDensity.current
    var lastPlayedCueId by remember { mutableStateOf(value = animationCue?.id) }

    LaunchedEffect(key1 = animationCue) {
        val cue = animationCue ?: return@LaunchedEffect
        if (cue.id == lastPlayedCueId) return@LaunchedEffect
        lastPlayedCueId = cue.id

        val gesture = cue.gestureFor(userId = userId) ?: return@LaunchedEffect
        gestureState.play(
            gesture = gesture,
            strikeDirection = if (isTopSeat) 1f else -1f,
            density = density,
            reactionDelayMillis = cue.defenderReactionDelayMillis(),
        )
    }

    return gestureState
}

/**
 * How long a defender holds still before reacting, so the shared reactions land in step with the
 * attacker's strike: a melee weapon (SS-192) spends its dash closing in before the blade lands, a
 * thrown weapon (SS-193) spends its flight time spinning across the gap, a Prepare Attack (SS-203)
 * spends its whole windup cocking the rapiers, and the generic lunges hit almost immediately.
 */
private fun CombatAnimationCue.defenderReactionDelayMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack -> normalAttackReactionDelayMillis()
        is CombatAnimationCue.PrepareAttack -> prepareAttackReactionDelayMillis()
        is CombatAnimationCue.Riposte -> RIPOSTE_STRIKE_MILLIS
        else -> REACTION_DELAY_MILLIS
    }

private fun CombatAnimationCue.NormalAttack.normalAttackReactionDelayMillis(): Long =
    when (attackerClassType) {
        CharacterClassType.Rogue -> MELEE_REACTION_DELAY_MILLIS
        CharacterClassType.Gambler -> THROW_REACTION_DELAY_MILLIS
        CharacterClassType.Duelist -> TWIN_SWORD_REACTION_DELAY_MILLIS
        CharacterClassType.Necromancer -> NECROMANCER_REAP_REACTION_DELAY_MILLIS
        CharacterClassType.Bard -> BARD_WAVE_REACTION_DELAY_MILLIS
        else -> REACTION_DELAY_MILLIS
    }

/**
 * A landed Prepare Attack collapses the target only as the blades arrive at the end of the long
 * windup; a whiffed one has the target sidestep as the thrust launches, clearing before it passes.
 */
private fun CombatAnimationCue.PrepareAttack.prepareAttackReactionDelayMillis(): Long =
    if (isSuccessful) PREPARE_ATTACK_STRIKE_MILLIS else PREPARE_ATTACK_WINDUP_MILLIS

/**
 * The longest a seated avatar animates for this cue — the striker's own gesture or a reactor's
 * delayed reaction, whichever comes to rest last — so the sequencer holds the flash callout until
 * the bodies stop moving (SS-201). Maxed with the weapon and effect overlays' own runs; a cue with
 * no seated motion beyond those overlays reports 0.
 */
internal fun CombatAnimationCue.seatedAvatarDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackReactionDelayMillis() + defenderReactionBodyMillis(outcome = outcome)

        is CombatAnimationCue.KillingMomentum -> REARM_BODY_MILLIS

        is CombatAnimationCue.Dodge ->
            if (didArmorIncrease) DODGE_BRACE_BODY_MILLIS else DODGE_FLOURISH_BODY_MILLIS

        is CombatAnimationCue.AllIn -> allInBodyMillis(outcome = outcome)

        is CombatAnimationCue.LuckyCoinFlip -> 0L

        is CombatAnimationCue.LuckyCoinHighRoller -> 0L

        is CombatAnimationCue.LuckyCoinHighRollerThrow ->
            REACTION_DELAY_MILLIS +
                if (isKnockout) COLLAPSE_BODY_MILLIS else FLINCH_HEAVY_BODY_MILLIS

        is CombatAnimationCue.PrepareAttack ->
            prepareAttackReactionDelayMillis() +
                if (isSuccessful) COLLAPSE_BODY_MILLIS else SIDESTEP_BODY_MILLIS

        is CombatAnimationCue.Riposte ->
            if (phase == RipostePhase.Landed) RIPOSTE_STRIKE_MILLIS + FLINCH_BODY_MILLIS else 0L

        is CombatAnimationCue.SummonMinion -> CAST_BODY_MILLIS
    }

/**
 * The shared defender reaction's body length per outcome, matching its flinch/sidestep/collapse.
 */
private fun defenderReactionBodyMillis(outcome: NormalRollMessageType): Long =
    when (outcome) {
        NormalRollMessageType.ArmorClassHit -> FLINCH_BODY_MILLIS
        NormalRollMessageType.ArmorClassMiss -> SIDESTEP_BODY_MILLIS
        NormalRollMessageType.Damage -> FLINCH_HEAVY_BODY_MILLIS
        NormalRollMessageType.Knockout -> COLLAPSE_BODY_MILLIS
    }

/**
 * The Gambler's own body during All-In: a self-knockout folds the toss into a collapse and runs the
 * longest, while the other outcomes settle back after the toss and the coin effect carries the
 * rest.
 */
private fun allInBodyMillis(outcome: AllInOutcome): Long =
    when (outcome) {
        AllInOutcome.SelfKnockout ->
            (ALL_IN_TOSS_MILLIS + ALL_IN_SETTLE_MILLIS).toLong() +
                ALL_IN_BUST_JOLT_MILLIS +
                COLLAPSE_BODY_MILLIS

        AllInOutcome.NoEffect,
        AllInOutcome.DoubleAttack -> ALL_IN_TOSS_GESTURE_MILLIS.toLong()
    }

private suspend fun AvatarGestureState.play(
    gesture: AvatarGesture,
    strikeDirection: Float,
    density: Density,
    reactionDelayMillis: Long,
) {
    snapToRest()
    with(density) {
        when (gesture) {
            AvatarGesture.Lunge ->
                lunge(distancePx = 48.dp.toPx() * strikeDirection, peakScale = 1.06f)

            AvatarGesture.LungeStrong ->
                lunge(distancePx = 68.dp.toPx() * strikeDirection, peakScale = 1.12f)

            AvatarGesture.LungeOvershoot ->
                lungeOvershoot(distancePx = 80.dp.toPx() * strikeDirection)

            AvatarGesture.Stab -> stab(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.StabFlurry ->
                stabFlurry(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.StabOvershoot ->
                stabOvershoot(dashPx = meleeDashDistance.toPx() * strikeDirection)

            AvatarGesture.Slash ->
                slashCombo(reachPx = 30.dp.toPx() * strikeDirection, isFinisher = false)

            AvatarGesture.SlashFinish ->
                slashCombo(reachPx = 30.dp.toPx() * strikeDirection, isFinisher = true)

            AvatarGesture.SlashWhiff -> slashWhiff(reachPx = 30.dp.toPx() * strikeDirection)

            AvatarGesture.Cast -> cast(reachPx = 14.dp.toPx() * strikeDirection)

            AvatarGesture.PreparedThrust -> preparedThrust(reachPx = 36.dp.toPx() * strikeDirection)

            AvatarGesture.PreparedThrustWhiff ->
                preparedThrustWhiff(reachPx = 36.dp.toPx() * strikeDirection)

            AvatarGesture.Throw ->
                throwFlick(flickPx = 26.dp.toPx() * strikeDirection, peakScale = 1.05f)

            AvatarGesture.ThrowStrong ->
                throwFlick(flickPx = 34.dp.toPx() * strikeDirection, peakScale = 1.1f)

            AvatarGesture.ThrowOvershoot -> throwOvershoot(flickPx = 32.dp.toPx() * strikeDirection)

            AvatarGesture.AllInToss -> allInToss(liftPx = 16.dp.toPx())

            AvatarGesture.AllInBust -> allInBust(liftPx = 16.dp.toPx())

            AvatarGesture.ReArm -> reArm(retreatPx = 30.dp.toPx() * -strikeDirection)

            AvatarGesture.Flinch ->
                flinch(
                    shakePx = 10.dp.toPx(),
                    tintPeak = 0.7f,
                    reactionDelayMillis = reactionDelayMillis,
                )

            AvatarGesture.FlinchHeavy ->
                flinchHeavy(shakePx = 16.dp.toPx(), reactionDelayMillis = reactionDelayMillis)

            AvatarGesture.Sidestep ->
                sidestep(distancePx = 40.dp.toPx(), reactionDelayMillis = reactionDelayMillis)

            AvatarGesture.DodgeFlourish -> dodgeFlourish(distancePx = 44.dp.toPx())

            AvatarGesture.DodgeBrace -> dodgeBrace(distancePx = 44.dp.toPx())

            AvatarGesture.Collapse -> collapse(reactionDelayMillis = reactionDelayMillis)
        }
    }
}

/**
 * The Necromancer's cast (SS-195): the summoned scythe does all the reaping, so the caster only
 * leans a little toward the defender and settles — no lunge, since the body never carries the blow.
 */
private suspend fun AvatarGestureState.cast(reachPx: Float) {
    offsetY.animateTo(targetValue = reachPx, animationSpec = tween(durationMillis = 180))
    offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
}

private suspend fun AvatarGestureState.lunge(distancePx: Float, peakScale: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 140))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280))
        }
        launch {
            stretchTo(targetX = peakScale, targetY = peakScale, durationMillis = 140)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 280)
        }
    }
}

private suspend fun AvatarGestureState.lungeOvershoot(distancePx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 180))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 420))
        }
        launch {
            rotation.animateTo(targetValue = 8f, animationSpec = tween(durationMillis = 180))
            rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

/**
 * The Rogue's close-combat pierce (SS-192): the avatar dashes into melee range of the defender —
 * the dagger of the weapon overlay riding in front — thrusts as the blade lands, holds the extended
 * pose while it sits embedded, and settles back to its seat. Timings mirror the overlay's
 * choreography so the body and the blade move as one.
 */
private suspend fun AvatarGestureState.stab(dashPx: Float) {
    dashIn(dashPx = dashPx)
    offsetY.animateTo(
        targetValue = dashPx * 1.12f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS),
    )
    delay(timeMillis = MELEE_EMBED_MILLIS)
    settleBack()
}

/** The knockout: the pierce, then the body driving the blade deeper before settling back. */
private suspend fun AvatarGestureState.stabFlurry(dashPx: Float) {
    dashIn(dashPx = dashPx)
    offsetY.animateTo(
        targetValue = dashPx * 1.12f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS),
    )
    delay(timeMillis = 80L)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx * 1.28f,
                animationSpec = tween(durationMillis = 120),
            )
        }
        launch { stretchTo(targetX = 0.94f, targetY = 1.08f, durationMillis = 120) }
    }
    delay(timeMillis = MELEE_EMBED_MILLIS)
    settleBack()
}

/**
 * The whiffed pierce: the dash carries in, but the thrust overextends past the sidestepping target
 * and the body tips off balance before recovering back to its seat.
 */
private suspend fun AvatarGestureState.stabOvershoot(dashPx: Float) {
    dashIn(dashPx = dashPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx * 1.3f,
                animationSpec = tween(durationMillis = 150),
            )
        }
        launch { rotation.animateTo(targetValue = 8f, animationSpec = tween(durationMillis = 150)) }
    }
    rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 150))
    settleBack()
}

/** The dash into melee range: a crouch that springs the body toward the defender's seat. */
private suspend fun AvatarGestureState.dashIn(dashPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = dashPx,
                animationSpec = tween(durationMillis = MELEE_DASH_MILLIS),
            )
        }
        launch {
            stretchTo(targetX = 1.04f, targetY = 0.94f, durationMillis = MELEE_DASH_MILLIS / 2)
            stretchTo(targetX = 0.97f, targetY = 1.05f, durationMillis = MELEE_DASH_MILLIS / 2)
        }
    }
}

private suspend fun AvatarGestureState.settleBack() {
    coroutineScope {
        launch { offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280)) }
        launch { stretchTo(targetX = 1f, targetY = 1f, durationMillis = 280) }
        launch { rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 280)) }
    }
}

/**
 * The Duelist's two-beat fence (SS-194): the body commits into the lead cut, then roughly half a
 * second later crosses the other way into the follow cut — the footwork behind the twin blades of
 * the weapon overlay. A finisher drives the second cut deeper. The beats are launched together so
 * the stagger matches the blades' one-two.
 */
private suspend fun AvatarGestureState.slashCombo(reachPx: Float, isFinisher: Boolean) {
    delay(timeMillis = twinSlashPreambleMillis())
    coroutineScope {
        launch { slashBeat(reachPx = reachPx, tiltDegrees = 7f) }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            val followReach = if (isFinisher) reachPx * 1.25f else reachPx
            slashBeat(reachPx = followReach, tiltDegrees = -7f)
        }
    }
}

/** The time both blades spend rising into their erect ready pose before the lead cut begins. */
private fun twinSlashPreambleMillis(): Long =
    (TWIN_SLASH_RAISE_MILLIS + TWIN_SLASH_READY_HOLD_MILLIS).toLong()

/** One fencing beat: a smooth commit toward the defender with a finesse tilt, then a recover. */
private suspend fun AvatarGestureState.slashBeat(reachPx: Float, tiltDegrees: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx,
                animationSpec =
                    tween(durationMillis = SLASH_LEAN_MILLIS, easing = FastOutSlowInEasing),
            )
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_RECOVER_MILLIS),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = tiltDegrees,
                animationSpec = tween(durationMillis = SLASH_LEAN_MILLIS),
            )
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_RECOVER_MILLIS),
            )
        }
        launch {
            stretchTo(targetX = 1.04f, targetY = 0.98f, durationMillis = SLASH_LEAN_MILLIS)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = SLASH_RECOVER_MILLIS)
        }
    }
}

/**
 * The parried combo: the lead cut lands as usual, but the follow cut overcommits past the
 * sidestepping defender and tips the body off balance before it recovers to its seat.
 */
private suspend fun AvatarGestureState.slashWhiff(reachPx: Float) {
    delay(timeMillis = twinSlashPreambleMillis())
    coroutineScope {
        launch { slashBeat(reachPx = reachPx, tiltDegrees = 7f) }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            slashOvershoot(reachPx = reachPx)
        }
    }
}

private suspend fun AvatarGestureState.slashOvershoot(reachPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx * 1.3f,
                animationSpec = tween(durationMillis = 150),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            rotation.animateTo(targetValue = 12f, animationSpec = tween(durationMillis = 150))
            rotation.animateTo(targetValue = -5f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

/**
 * The Duelist's Prepare Attack (SS-203): the body coils back while the paired rapiers of the effect
 * overlay twirl and gleam, then springs forward in step with their thrust and settles once the
 * strike lands.
 */
private suspend fun AvatarGestureState.preparedThrust(reachPx: Float) {
    prepareCoil(reachPx = reachPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx,
                animationSpec =
                    tween(durationMillis = PREPARE_THRUST_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            stretchTo(targetX = 0.96f, targetY = 1.06f, durationMillis = PREPARE_THRUST_MILLIS)
        }
    }
    settleBack()
}

/**
 * The whiffed Prepare Attack: the same coil and spring, but the thrust overcommits past the
 * sidestepping target and tips the body off balance before it recovers to its seat.
 */
private suspend fun AvatarGestureState.preparedThrustWhiff(reachPx: Float) {
    prepareCoil(reachPx = reachPx)
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = reachPx * 1.3f,
                animationSpec =
                    tween(durationMillis = PREPARE_THRUST_MILLIS, easing = FastOutLinearInEasing),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            rotation.animateTo(
                targetValue = 10f,
                animationSpec = tween(durationMillis = PREPARE_THRUST_MILLIS),
            )
            rotation.animateTo(targetValue = -4f, animationSpec = tween(durationMillis = 160))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
        launch { stretchTo(targetX = 1f, targetY = 1f, durationMillis = 300) }
    }
}

/**
 * The Prepare Attack windup: the body sinks into a coiled crouch away from the target while the
 * blades appear, twirl their full revolution, and pull back, then holds while the gleam runs.
 */
private suspend fun AvatarGestureState.prepareCoil(reachPx: Float) {
    val coilMillis = PREPARE_APPEAR_MILLIS + PREPARE_TWIRL_MILLIS + PREPARE_PULL_BACK_MILLIS
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * reachPx,
                animationSpec = tween(durationMillis = coilMillis, easing = FastOutSlowInEasing),
            )
        }
        launch { stretchTo(targetX = 1.05f, targetY = 0.94f, durationMillis = coilMillis) }
    }
    delay(timeMillis = PREPARE_GLEAM_MILLIS.toLong())
}

/**
 * The Gambler's card throw (SS-193): a planted flick — the body leans back into a wind-up, then
 * snaps forward as the card is loosed, and settles. Smaller and quicker than a lunge, since the
 * thrown card of the weapon overlay does the travelling, not the body.
 */
private suspend fun AvatarGestureState.throwFlick(flickPx: Float, peakScale: Float) {
    val leanDirection = if (flickPx >= 0f) 1f else -1f
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * flickPx,
                animationSpec = tween(durationMillis = 110),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = -5f * leanDirection,
                animationSpec = tween(durationMillis = 110),
            )
        }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = flickPx, animationSpec = tween(durationMillis = 90))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 260))
        }
        launch {
            rotation.animateTo(
                targetValue = 4f * leanDirection,
                animationSpec = tween(durationMillis = 90),
            )
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 260))
        }
        launch {
            stretchTo(targetX = peakScale, targetY = peakScale, durationMillis = 90)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 260)
        }
    }
}

/**
 * The whiffed throw: the flick overcommits past the sidestepping target, tipping the body off
 * balance before it recovers back to its seat.
 */
private suspend fun AvatarGestureState.throwOvershoot(flickPx: Float) {
    val leanDirection = if (flickPx >= 0f) 1f else -1f
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = -0.4f * flickPx,
                animationSpec = tween(durationMillis = 110),
            )
        }
        launch {
            rotation.animateTo(
                targetValue = -6f * leanDirection,
                animationSpec = tween(durationMillis = 110),
            )
        }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = flickPx * 1.2f,
                animationSpec = tween(durationMillis = 100),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 320))
        }
        launch {
            rotation.animateTo(
                targetValue = 10f * leanDirection,
                animationSpec = tween(durationMillis = 100),
            )
            rotation.animateTo(
                targetValue = -4f * leanDirection,
                animationSpec = tween(durationMillis = 160),
            )
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

/**
 * The All-In toss (SS-193): the Gambler dips to gather their winnings, then flings them skyward —
 * the coins the effect overlay throws — and settles back to watch how they land.
 */
private suspend fun AvatarGestureState.allInToss(liftPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = liftPx * 0.5f,
                animationSpec = tween(durationMillis = 150),
            )
        }
        launch { stretchTo(targetX = 1.04f, targetY = 0.95f, durationMillis = 150) }
    }
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = -liftPx, animationSpec = tween(durationMillis = 170))
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            stretchTo(targetX = 0.96f, targetY = 1.06f, durationMillis = 170)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 300)
        }
    }
}

/**
 * The All-In bust (SS-193): the toss, a beat while the pile gathers, then the blast jolts the
 * Gambler off balance before they fold into the same collapse a knockout leaves behind. The wait
 * lines the jolt up with the overlay's detonation — the pile finishes landing and settling first.
 */
private suspend fun AvatarGestureState.allInBust(liftPx: Float) {
    allInToss(liftPx = liftPx)
    delay(
        timeMillis =
            (ALL_IN_TOSS_MILLIS + ALL_IN_SETTLE_MILLIS - ALL_IN_TOSS_GESTURE_MILLIS).toLong()
    )
    coroutineScope {
        launch {
            offsetY.animateTo(
                targetValue = liftPx * 0.4f,
                animationSpec = tween(durationMillis = 70),
            )
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 90))
        }
        launch {
            stretchTo(targetX = 1.08f, targetY = 0.9f, durationMillis = 70)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 90)
        }
    }
    collapse(reactionDelayMillis = 0L)
}

/**
 * The Killing Momentum re-arm (SS-192): the attacker springs back off the downed target into a low
 * crouch, holds poised for a beat, then pops back up ready for the follow-up attack — reading as
 * one continuous assault while the collapsed target stays down.
 */
private suspend fun AvatarGestureState.reArm(retreatPx: Float) {
    coroutineScope {
        launch {
            offsetY.animateTo(targetValue = retreatPx, animationSpec = tween(durationMillis = 140))
        }
        launch { stretchTo(targetX = 1.08f, targetY = 0.86f, durationMillis = 140) }
    }
    delay(timeMillis = 180L)
    coroutineScope {
        launch { offsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 150)) }
        launch {
            stretchTo(targetX = 0.95f, targetY = 1.08f, durationMillis = 150)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 180)
        }
    }
}

/**
 * The Dodge flourish (SS-192): a taunting weave — the Rogue whips out wide, crosses back through
 * center, and settles — sharper and showier than the plain sidestep that answered the miss itself.
 */
private suspend fun AvatarGestureState.dodgeFlourish(distancePx: Float) {
    coroutineScope {
        launch {
            offsetX.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 110))
            offsetX.animateTo(
                targetValue = -0.35f * distancePx,
                animationSpec = tween(durationMillis = 180),
            )
            offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 160))
        }
        launch {
            rotation.animateTo(targetValue = -9f, animationSpec = tween(durationMillis = 110))
            rotation.animateTo(targetValue = 5f, animationSpec = tween(durationMillis = 180))
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 160))
        }
    }
}

/**
 * The armor-gaining Dodge: the flourish, then a hardening accent — a steel-blue glint with a
 * momentary swell — for the +1 Armor the dodge just banked.
 */
private suspend fun AvatarGestureState.dodgeBrace(distancePx: Float) {
    dodgeFlourish(distancePx = distancePx)
    coroutineScope {
        launch {
            armorTint.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 90))
            armorTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 320))
        }
        launch {
            stretchTo(targetX = 1.05f, targetY = 1.05f, durationMillis = 90)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 320)
        }
    }
}

private suspend fun AvatarGestureState.flinch(
    shakePx: Float,
    tintPeak: Float,
    reactionDelayMillis: Long,
) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch { shake(shakePx, -0.7f * shakePx, 0.4f * shakePx, 0f) }
        launch {
            hurtTint.animateTo(targetValue = tintPeak, animationSpec = tween(durationMillis = 80))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
    }
}

private suspend fun AvatarGestureState.flinchHeavy(shakePx: Float, reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch { shake(shakePx, -0.8f * shakePx, 0.5f * shakePx, -0.25f * shakePx, 0f) }
        launch {
            hurtTint.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 90))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 380))
        }
        launch {
            stretchTo(targetX = 1.06f, targetY = 0.9f, durationMillis = 110)
            stretchTo(targetX = 1f, targetY = 1f, durationMillis = 240)
        }
    }
}

private suspend fun AvatarGestureState.sidestep(distancePx: Float, reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch {
            offsetX.animateTo(targetValue = distancePx, animationSpec = tween(durationMillis = 120))
            delay(timeMillis = 200L)
            offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
        launch {
            rotation.animateTo(targetValue = 6f, animationSpec = tween(durationMillis = 120))
            delay(timeMillis = 200L)
            rotation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }
}

private suspend fun AvatarGestureState.collapse(reactionDelayMillis: Long) {
    delay(timeMillis = reactionDelayMillis)
    coroutineScope {
        launch {
            hurtTint.animateTo(targetValue = 0.8f, animationSpec = tween(durationMillis = 90))
            hurtTint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500))
        }
        launch {
            collapseFraction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450),
            )
        }
        launch {
            saturation.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 650))
        }
    }
}

private suspend fun AvatarGestureState.shake(vararg targetsPx: Float) {
    for (target in targetsPx) {
        offsetX.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = SHAKE_SEGMENT_MILLIS),
        )
    }
}

private suspend fun AvatarGestureState.stretchTo(
    targetX: Float,
    targetY: Float,
    durationMillis: Int,
) {
    coroutineScope {
        launch {
            scaleX.animateTo(
                targetValue = targetX,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
        launch {
            scaleY.animateTo(
                targetValue = targetY,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
    }
}

private suspend fun AvatarGestureState.snapToRest() {
    offsetX.snapTo(targetValue = 0f)
    offsetY.snapTo(targetValue = 0f)
    rotation.snapTo(targetValue = 0f)
    scaleX.snapTo(targetValue = 1f)
    scaleY.snapTo(targetValue = 1f)
    collapseFraction.snapTo(targetValue = 0f)
    hurtTint.snapTo(targetValue = 0f)
    armorTint.snapTo(targetValue = 0f)
    saturation.snapTo(targetValue = 1f)
}

private fun armorTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - ARMOR_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                ARMOR_TINT_MAX_BLUE_OFFSET * tint,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}

private fun hurtTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - HURT_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                HURT_TINT_MAX_RED_OFFSET * tint,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}
