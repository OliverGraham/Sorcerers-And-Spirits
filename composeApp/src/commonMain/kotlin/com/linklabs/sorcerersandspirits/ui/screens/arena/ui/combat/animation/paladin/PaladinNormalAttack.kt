package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.lerpAngle
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.slashPoint
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.slashTangent
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.smoothstep
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MACE_RAISE_MILLIS = 200
private const val MACE_READY_HOLD_MILLIS = 200L
private const val MACE_SWING_MILLIS = 300
private const val MACE_END_HOLD_MILLIS = 120L
private const val MACE_FADE_MILLIS = 200
private const val MACE_SWING_CONTACT_MILLIS = MACE_SWING_MILLIS * 82L / 100L
private const val MACE_ENGAGE_FRACTION = 0.3f
private const val MACE_TRAIL_SPAN = 0.5f
private const val MACE_TRAIL_SAMPLES = 14
private const val MACE_COCK_DEGREES = 196f

private val maceWindupLift = 118.dp
private val maceWindupBack = 26.dp
private val maceSwingBow = 54.dp

internal const val PALADIN_SWING_REACTION_DELAY_MILLIS: Long =
    MACE_RAISE_MILLIS.toLong() + MACE_READY_HOLD_MILLIS + MACE_SWING_CONTACT_MILLIS

@Composable
internal fun PaladinNormalAttackOverlay(
    animatingMessage: ArenaMessageState.NormalRoll,
    attackerBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
    accentColor: Color = paladinEssenceGreen,
) {
    val paladinStrikeState = remember { PaladinStrikeState() }
    val density = LocalDensity.current
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val messageType = animatingMessage.normalRollMessageType

        paladinStrikeState.play(
            geometry =
                weaponStrikeGeometry(
                    attackerBounds = attackerBounds,
                    target = defenderBounds.center,
                    accentColor = accentColor,
                    density = density,
                ),
            normalRollMessageType = messageType,
        )

        onAvatarAnimationComplete()
    }

    Canvas(modifier = modifier) { drawMaceSwing(state = paladinStrikeState) }
}

// TODO: I had meant this to be related to the actual dice total value
private fun essenceFor(normalRollMessageType: NormalRollMessageType): Float =
    when (normalRollMessageType) {
        NormalRollMessageType.Knockout -> 1f
        NormalRollMessageType.Damage -> 0.72f
        NormalRollMessageType.ArmorClassHit -> 0.48f
        NormalRollMessageType.ArmorClassMiss -> 0.28f
    }

// TODO: I had meant this to be related to the actual dice total value
private fun burstScaleFor(normalRollMessageType: NormalRollMessageType): Float =
    when (normalRollMessageType) {
        NormalRollMessageType.Knockout -> 1.3f
        NormalRollMessageType.Damage -> 1f
        NormalRollMessageType.ArmorClassHit -> 0.8f
        NormalRollMessageType.ArmorClassMiss -> 0f
    }

internal suspend fun PaladinStrikeState.play(
    geometry: WeaponStrikeGeometry,
    normalRollMessageType: NormalRollMessageType,
) {
    swing.snapTo(targetValue = 0f)
    alpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry
    this.essence = essenceFor(normalRollMessageType = normalRollMessageType)

    val connects = normalRollMessageType != NormalRollMessageType.ArmorClassMiss

    alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = MACE_RAISE_MILLIS))
    delay(timeMillis = MACE_READY_HOLD_MILLIS)

    coroutineScope {
        launch {
            swing.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = MACE_SWING_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            if (connects) {
                delay(timeMillis = MACE_SWING_CONTACT_MILLIS)
                impactBurst(
                    impactAlpha = impactAlpha,
                    impactScale = impactScale,
                    peakScale = burstScaleFor(normalRollMessageType = normalRollMessageType),
                )
            }
        }
    }

    delay(timeMillis = MACE_END_HOLD_MILLIS)
    alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = MACE_FADE_MILLIS))

    this.scene = null
}

internal fun DrawScope.drawMaceSwing(state: PaladinStrikeState) {
    val geometry = state.scene ?: return
    val alpha = state.alpha.value
    if (alpha <= 0f) return

    val axisVector = geometry.target - geometry.start
    val axisLength = axisVector.getDistance()
    if (axisLength == 0f) return
    val axis = axisVector / axisLength

    val windupHead =
        geometry.start - axis * maceWindupBack.toPx() + Offset(x = 0f, y = -maceWindupLift.toPx())
    val target = geometry.target

    val swingVector = target - windupHead
    val swingLength = swingVector.getDistance()
    if (swingLength == 0f) return
    val swingAxis = swingVector / swingLength
    val swingPerp = Offset(x = -swingAxis.y, y = swingAxis.x)
    val bowsForward = swingPerp.x * axis.x + swingPerp.y * axis.y >= 0f
    val bowDir = if (bowsForward) swingPerp else -swingPerp
    val bow = maceSwingBow.toPx()

    val sweep = state.swing.value
    val engage = smoothstep(edge0 = 0f, edge1 = MACE_ENGAGE_FRACTION, value = sweep)
    val position =
        slashPoint(start = windupHead, end = target, perp = bowDir, bow = bow, fraction = sweep)

    if (engage > 0f) {
        drawEssenceTrail(
            start = windupHead,
            end = target,
            perp = bowDir,
            bow = bow,
            sweep = sweep,
            essence = state.essence,
            alpha = alpha * engage,
        )
    }

    val tangent =
        slashTangent(start = windupHead, end = target, perp = bowDir, bow = bow, fraction = sweep)
    val swingDegrees = atan2(y = tangent.y, x = tangent.x) * 180f / PI.toFloat() - 90f
    val degrees =
        lerpAngle(fromDegrees = MACE_COCK_DEGREES, toDegrees = swingDegrees, fraction = engage)
    rotate(degrees = degrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawMaceShape(essence = state.essence, alpha = alpha)
        }
    }

    val impactAlpha = state.impactAlpha.value
    val isBursting = impactAlpha > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(
            center = geometry.target,
            scale = state.impactScale.value,
            alpha = impactAlpha,
            accentColor = geometry.accentColor,
        )
    }
}

private fun DrawScope.drawEssenceTrail(
    start: Offset,
    end: Offset,
    perp: Offset,
    bow: Float,
    sweep: Float,
    essence: Float,
    alpha: Float,
) {
    val trailStart = (sweep - MACE_TRAIL_SPAN).coerceAtLeast(minimumValue = 0f)
    if (sweep <= trailStart) return

    val plume =
        Path().apply {
            for (sample in 0..MACE_TRAIL_SAMPLES) {
                val fraction = trailStart + (sweep - trailStart) * sample / MACE_TRAIL_SAMPLES
                val point =
                    slashPoint(
                        start = start,
                        end = end,
                        perp = perp,
                        bow = bow,
                        fraction = fraction,
                    )
                if (sample == 0) {
                    moveTo(x = point.x, y = point.y)
                } else {
                    lineTo(x = point.x, y = point.y)
                }
            }
        }
    val tail = slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = trailStart)
    val head = slashPoint(start = start, end = end, perp = perp, bow = bow, fraction = sweep)
    val widthScale = 0.65f + 0.55f * essence

    drawPath(
        path = plume,
        color = paladinEssenceGlow,
        style =
            Stroke(
                width = 32.dp.toPx() * widthScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        alpha = alpha * essence * 0.18f,
    )
    drawPath(
        path = plume,
        color = paladinEssenceGreen,
        style =
            Stroke(
                width = 19.dp.toPx() * widthScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        alpha = alpha * essence * 0.32f,
    )
    drawPath(
        path = plume,
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, paladinEssenceGreen, paladinEssenceCore),
                start = tail,
                end = head,
            ),
        style =
            Stroke(
                width = 10.dp.toPx() * widthScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        alpha = alpha * (0.5f + 0.45f * essence),
    )
}

@Stable
internal class PaladinStrikeState {
    val swing = Animatable(initialValue = 0f)
    val alpha = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
    var essence by mutableStateOf(value = 0f)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinNormalAttackKnockoutPreview() {
    PaladinNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinNormalAttackDamagePreview() {
    PaladinNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinNormalAttackArmorClassHitPreview() {
    PaladinNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PaladinNormalAttackArmorClassMissPreview() {
    PaladinNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun PaladinNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "paladin-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Paladin,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Paladin, seats = seats) {
        PaladinNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
