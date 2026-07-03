package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.animation.core.Animatable
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AvatarGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.combatWeapon
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.gestureFor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REACTION_DELAY_MILLIS = 170L
private const val SHAKE_SEGMENT_MILLIS = 55
private const val COLLAPSE_ROTATION_DEGREES = 80f
private const val COLLAPSE_FEET_SLIDE_FRACTION = 0.4f
private const val HURT_TINT_MAX_RED_OFFSET = 130f
private const val HURT_TINT_CHANNEL_DAMP = 0.45f
private const val ARMOR_TINT_MAX_BLUE_OFFSET = 110f
private const val ARMOR_TINT_CHANNEL_DAMP = 0.3f

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
 * attacker's strike: a weapon melee strike (SS-192) spends its dash closing in before the blade
 * lands, while the generic lunges hit almost immediately.
 */
private fun CombatAnimationCue.defenderReactionDelayMillis(): Long {
    val isWeaponMeleeStrike =
        this is CombatAnimationCue.NormalAttack && attackerClassType.combatWeapon != null
    return if (isWeaponMeleeStrike) MELEE_REACTION_DELAY_MILLIS else REACTION_DELAY_MILLIS
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
