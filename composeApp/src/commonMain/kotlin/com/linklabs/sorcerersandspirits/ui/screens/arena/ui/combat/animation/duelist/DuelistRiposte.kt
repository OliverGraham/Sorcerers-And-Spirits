package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.lerpAngle
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

private val upright = Offset(x = 0f, y = -1f)

internal const val RIPOSTE_STRIKE_MILLIS = (RIPOSTE_PARRY_MILLIS + RIPOSTE_STAB_MILLIS).toLong()


// TODO: Riposte Animation needs some work, visually

@Composable
internal fun RiposteOverlay(
    riposteMessage: ArenaMessageState.Riposte,
    duelistBounds: Rect,
    attackerBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { RiposteEffectState() }
    val density = LocalDensity.current
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = riposteMessage.id) {
        val scene =
            riposteScene(
                duelistBounds = duelistBounds,
                attackerBounds = attackerBounds,
                accentColor = CharacterClassType.Duelist.toColor(),
                density = density,
            ) ?: return@LaunchedEffect

        state.play(scene = scene, message = riposteMessage)
        onAvatarAnimationComplete()
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

private suspend fun RiposteEffectState.play(
    scene: RiposteScene,
    message: ArenaMessageState.Riposte,
) {
    when (message) {
        is ArenaMessageState.Riposte.EnGarde ->
            resolve(scene = scene, isLanded = message.isSuccessful)

        is ArenaMessageState.Riposte.Trigger -> raiseIntoGuard(scene = scene)
    }
}

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

private fun DrawScope.drawParryBlade(scene: RiposteScene, state: RiposteEffectState) {
    val alpha = state.bladeAlpha.value
    if (alpha <= 0f) return

    val progress = state.parry.value
    val deflectDegrees = sin(x = (progress * PI)).toFloat() * PARRY_DEFLECT_DEGREES
    val vibrationDegrees =
        sin(x = (progress * PI * PARRY_VIBRATION_CYCLES)).toFloat() *
            PARRY_VIBRATION_DEGREES *
            (1f - progress)
    val tipDirection = upright.rotatedBy(degrees = deflectDegrees + vibrationDegrees)
    drawBlade(hand = scene.parryHand, tipDirection = tipDirection, scene = scene, alpha = alpha)
}

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
    val parry = Animatable(initialValue = 0f)
    val stab = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

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
    DuelistRipostePreview(resolution = RipostePhase.Landed)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistRiposteMissedPreview() {
    DuelistRipostePreview(resolution = RipostePhase.Missed)
}

// TODO: Fix previews
@Composable
private fun DuelistRipostePreview(resolution: RipostePhase) {
    val seats = rememberDuoSeats()
    val riposteMessage =
        rememberReplayingCues(replayKey = resolution) { iteration ->
            listOf(
                ArenaMessageState.Riposte.Trigger(
                    id = "riposte-ready-$iteration",
                    duelistUserId = PREVIEW_ACTOR_ID,
                    attackerUserId = PREVIEW_TARGET_ID,
                ),
                ArenaMessageState.Riposte.EnGarde(
                    id = "riposte-resolve-$iteration",
                    duelistUserId = PREVIEW_ACTOR_ID,
                    attackerUserId = PREVIEW_TARGET_ID,
                    animation = ArenaAnimationState.FlashAndAvatar,
                    firstDie = 4,
                    secondDie = 2,
                    isSuccessful = true,
                    gestures = emptyMap(),
                ),
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Duelist, seats = seats) {
        RiposteOverlay(
            riposteMessage = riposteMessage,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
