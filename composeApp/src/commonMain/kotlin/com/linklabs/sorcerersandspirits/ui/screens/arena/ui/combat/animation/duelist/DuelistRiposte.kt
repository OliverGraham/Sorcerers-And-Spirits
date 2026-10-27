package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.lerpAngle
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal const val RIPOSTE_APPEAR_MILLIS = 200
internal const val RIPOSTE_STAB_MILLIS = 200
private const val RIPOSTE_EMBED_MILLIS = 150L
private const val RIPOSTE_FADE_MILLIS = 220

private const val PARRY_ANTICIPATION_MILLIS = 90
private const val PARRY_SWEEP_PEAK_MILLIS = 240
private const val PARRY_ARM_MILLIS = 330
private const val PARRY_BLADE_LAG_MILLIS = 60
private const val PARRY_BLADE_MILLIS = 300

internal const val RIPOSTE_PARRY_MILLIS = PARRY_BLADE_LAG_MILLIS + PARRY_BLADE_MILLIS

private const val PARRY_GUARD_DEGREES = 26f
private const val PARRY_SWEEP_END_DEGREES = 118f
private const val PARRY_ARM_ANTICIPATION = -0.12f
private const val PARRY_ARM_OVERSHOOT = 1.08f
private const val PARRY_BLADE_ANTICIPATION = -0.18f
private const val PARRY_BLADE_OVERSHOOT = 1.16f
private val parrySweepLateralTravel = 72.dp
private val parrySweepDrop = 38.dp
private const val COUNTER_WINDUP_DEGREES = 38f
private const val MISS_OVERSHOOT_TRAVEL = 1.3f
private const val RIPOSTE_BURST_PEAK_SCALE = 1.2f

private val parrySwayLateralTravel = 7.dp
private val parrySwayVerticalTravel = 4.dp
private const val PARRY_SWAY_ANGLE_DEGREES = 5f
private const val PARRY_SWAY_LATERAL_MILLIS = 820
private const val PARRY_SWAY_VERTICAL_MILLIS = 1100
private const val PARRY_SWAY_ANGLE_MILLIS = 940

private val counterSwayLateralTravel = 3.dp
private val counterSwayVerticalTravel = 2.5.dp
private const val COUNTER_SWAY_ANGLE_DEGREES = 3f
private const val COUNTER_SWAY_LATERAL_MILLIS = 1300
private const val COUNTER_SWAY_VERTICAL_MILLIS = 1000
private const val COUNTER_SWAY_ANGLE_MILLIS = 1180

private val upright = Offset(x = 0f, y = -1f)

internal const val RIPOSTE_STRIKE_MILLIS = (RIPOSTE_PARRY_MILLIS + RIPOSTE_STAB_MILLIS).toLong()

@Composable
internal fun RiposteOverlay(
    riposteMessage: ArenaMessageState.Riposte,
    duelistBounds: Rect,
    attackerBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { RiposteEffectState() }
    val density = LocalDensity.current
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = riposteMessage.id) {
        val scene =
            riposteScene(
                duelistBounds = duelistBounds,
                attackerBounds = attackerBounds,
                accentColor = CharacterClassType.Duelist.toColor(),
                density = density,
            ) ?: return@LaunchedEffect

        val completion =
            AnimationCompleteArguments(
                messageId = riposteMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            )

        when (riposteMessage) {
            is ArenaMessageState.Riposte.Trigger -> {
                state.raiseIntoGuard(scene = scene)
                onAnimationComplete(completion)
                state.idleGuardSway(scene = scene)
            }

            is ArenaMessageState.Riposte.EnGarde -> {
                state.resolve(scene = scene, isLanded = riposteMessage.isSuccessful)
                onAnimationComplete(completion)
            }
        }
    }

    Canvas(modifier = modifier) { drawRiposte(state = state) }
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

private suspend fun RiposteEffectState.raiseIntoGuard(scene: RiposteScene) {
    parryArm.snapTo(targetValue = 0f)
    parryBlade.snapTo(targetValue = 0f)
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

private suspend fun RiposteEffectState.idleGuardSway(scene: RiposteScene) = coroutineScope {
    this@idleGuardSway.scene = scene

    launch { parrySwayLateral.sway(startValue = -1f, durationMillis = PARRY_SWAY_LATERAL_MILLIS) }
    launch { parrySwayVertical.sway(startValue = 1f, durationMillis = PARRY_SWAY_VERTICAL_MILLIS) }
    launch { parrySwayAngle.sway(startValue = -1f, durationMillis = PARRY_SWAY_ANGLE_MILLIS) }
    launch { counterSwayLateral.sway(startValue = 1f, durationMillis = COUNTER_SWAY_LATERAL_MILLIS) }
    launch {
        counterSwayVertical.sway(startValue = -1f, durationMillis = COUNTER_SWAY_VERTICAL_MILLIS)
    }
    launch { counterSwayAngle.sway(startValue = 1f, durationMillis = COUNTER_SWAY_ANGLE_MILLIS) }
}

private suspend fun Animatable<Float, *>.sway(startValue: Float, durationMillis: Int) {
    snapTo(targetValue = startValue)
    animateTo(
        targetValue = -startValue,
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
}

private suspend fun RiposteEffectState.settleSway() {
    parrySwayLateral.snapTo(targetValue = 0f)
    parrySwayVertical.snapTo(targetValue = 0f)
    parrySwayAngle.snapTo(targetValue = 0f)
    counterSwayLateral.snapTo(targetValue = 0f)
    counterSwayVertical.snapTo(targetValue = 0f)
    counterSwayAngle.snapTo(targetValue = 0f)
}

private suspend fun RiposteEffectState.resolve(scene: RiposteScene, isLanded: Boolean) {
    this.scene = scene
    bladeAlpha.snapTo(targetValue = 1f)
    parryArm.snapTo(targetValue = 0f)
    parryBlade.snapTo(targetValue = 0f)
    stab.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    settleSway()

    sweepParry()

    if (isLanded) {
        land()
    } else {
        sailPast()
    }

    this.scene = null
}

private suspend fun RiposteEffectState.sweepParry() = coroutineScope {
    launch {
        parryArm.animateTo(
            targetValue = 1f,
            animationSpec =
            keyframes {
                durationMillis = PARRY_ARM_MILLIS
                0f at 0 using LinearOutSlowInEasing
                PARRY_ARM_ANTICIPATION at PARRY_ANTICIPATION_MILLIS using FastOutSlowInEasing
                PARRY_ARM_OVERSHOOT at PARRY_SWEEP_PEAK_MILLIS using LinearOutSlowInEasing
            },
        )
    }
    launch {
        delay(timeMillis = PARRY_BLADE_LAG_MILLIS.toLong())
        parryBlade.animateTo(
            targetValue = 1f,
            animationSpec =
            keyframes {
                durationMillis = PARRY_BLADE_MILLIS
                0f at 0 using LinearOutSlowInEasing
                PARRY_BLADE_ANTICIPATION at PARRY_ANTICIPATION_MILLIS using FastOutSlowInEasing
                PARRY_BLADE_OVERSHOOT at PARRY_SWEEP_PEAK_MILLIS using LinearOutSlowInEasing
            },
        )
    }
}

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

private suspend fun RiposteEffectState.sailPast() {
    coroutineScope {
        launch {
            stab.animateTo(
                targetValue = MISS_OVERSHOOT_TRAVEL,
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

private fun DrawScope.drawParryBlade(scene: RiposteScene, state: RiposteEffectState) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val armProgress = state.parryArm.value
    val bladeProgress = state.parryBlade.value
    val sweepDegrees =
        -PARRY_GUARD_DEGREES - bladeProgress * (PARRY_SWEEP_END_DEGREES - PARRY_GUARD_DEGREES) +
            state.parrySwayAngle.value * PARRY_SWAY_ANGLE_DEGREES
    val tipDirection = upright.rotatedBy(degrees = sweepDegrees)

    val lateral = Offset(x = -scene.axis.y, y = scene.axis.x)
    val handSweep =
        lateral * (-parrySweepLateralTravel.toPx() * armProgress) +
            Offset(x = 0f, y = parrySweepDrop.toPx() * armProgress)
    val sway =
        lateral * (parrySwayLateralTravel.toPx() * state.parrySwayLateral.value) +
            Offset(x = 0f, y = parrySwayVerticalTravel.toPx() * state.parrySwayVertical.value)
    val hand = scene.parryHand + handSweep + sway
    drawBlade(hand = hand, tipDirection = tipDirection, scene = scene, alpha = alpha)
}

private fun DrawScope.drawCounterBlade(scene: RiposteScene, state: RiposteEffectState) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val travel = state.stab.value
    val swingFraction = travel.coerceAtMost(maximumValue = 1f)
    val axisDegrees = atan2(y = scene.axis.y, x = scene.axis.x) * 180f / PI.toFloat()
    val windupDirection = upright.rotatedBy(degrees = COUNTER_WINDUP_DEGREES)
    val windupDegrees = atan2(y = windupDirection.y, x = windupDirection.x) * 180f / PI.toFloat()
    val degrees =
        lerpAngle(fromDegrees = windupDegrees, toDegrees = axisDegrees, fraction = swingFraction) +
            state.counterSwayAngle.value * COUNTER_SWAY_ANGLE_DEGREES
    val radians = degrees * PI.toFloat() / 180f
    val tipDirection = Offset(x = cos(x = radians), y = sin(x = radians))

    val lateral = Offset(x = -scene.axis.y, y = scene.axis.x)
    val sway =
        lateral * (counterSwayLateralTravel.toPx() * state.counterSwayLateral.value) +
            Offset(x = 0f, y = counterSwayVerticalTravel.toPx() * state.counterSwayVertical.value)
    val landedHand = scene.strikePoint - scene.axis * scene.bladeLengthPx
    val hand = lerp(start = scene.attackHand, stop = landedHand, fraction = travel) + sway
    drawBlade(hand = hand, tipDirection = tipDirection, scene = scene, alpha = alpha)
}

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

@Stable
private class RiposteEffectState {
    val bladeAlpha = Animatable(initialValue = 0f)
    val parryArm = Animatable(initialValue = 0f)
    val parryBlade = Animatable(initialValue = 0f)
    val stab = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    val parrySwayLateral = Animatable(initialValue = 0f)
    val parrySwayVertical = Animatable(initialValue = 0f)
    val parrySwayAngle = Animatable(initialValue = 0f)
    val counterSwayLateral = Animatable(initialValue = 0f)
    val counterSwayVertical = Animatable(initialValue = 0f)
    val counterSwayAngle = Animatable(initialValue = 0f)

    var scene by mutableStateOf<RiposteScene?>(value = null)
}

@Immutable
private data class RiposteScene(
    val axis: Offset,
    val parryHand: Offset,
    val attackHand: Offset,
    val strikePoint: Offset,
    val bladeLengthPx: Float,
    val accentColor: Color,
)

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteLandedPreview() {
    DuelistRipostePreview(isSuccessful = true)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteMissedPreview() {
    DuelistRipostePreview(isSuccessful = false)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteGuardSwayPreview() {
    val avatars = rememberDuoAvatars()
    AnimationPreview(
        accentClass = CharacterClassType.Duelist,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Riposte.Trigger(
                id = id,
                duelistUserId = PREVIEW_ACTOR_ID,
                attackerUserId = PREVIEW_TARGET_ID,
                attackerClassName = "Rogue",
                attackerClassType = CharacterClassType.Rogue,
                successRangeMin = 6,
                successRangeMax = 8,
            )
        },
    ) { message ->
        RiposteOverlay(
            riposteMessage = message,
            duelistBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            attackerBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DuelistRipostePreview(isSuccessful: Boolean) {
    val avatars = rememberDuoAvatars()
    AnimationPreview(
        accentClass = CharacterClassType.Duelist,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.Riposte.EnGarde(
                id = id,
                duelistUserId = PREVIEW_ACTOR_ID,
                attackerUserId = PREVIEW_TARGET_ID,
                attackerClassName = "Rogue",
                attackerClassType = CharacterClassType.Rogue,
                firstDie = if (isSuccessful) 4 else 6,
                secondDie = if (isSuccessful) 3 else 6,
                total = if (isSuccessful) 7 else 12,
                isSuccessful = isSuccessful,
                gestures = emptyMap(),
            )
        },
    ) { message ->
        RiposteOverlay(
            riposteMessage = message,
            duelistBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            attackerBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
