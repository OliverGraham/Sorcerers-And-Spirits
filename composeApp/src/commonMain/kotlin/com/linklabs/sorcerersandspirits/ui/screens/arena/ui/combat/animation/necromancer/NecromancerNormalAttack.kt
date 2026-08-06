package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

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
import androidx.compose.ui.geometry.lerp
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val SCYTHE_SUMMON_MILLIS = 260
private const val SCYTHE_GLOW_HOLD_MILLIS = 240L
private const val SCYTHE_RISE_MILLIS = 220
private const val SCYTHE_REAP_MILLIS = 320
private const val SCYTHE_REAP_END_HOLD_MILLIS = 120L
private const val SCYTHE_FADE_MILLIS = 200

private const val SCYTHE_REAP_CONTACT_MILLIS: Long = SCYTHE_REAP_MILLIS * 56L / 100L
private const val SCYTHE_SLASH_TRAIL_SPAN = 0.55f
private const val SCYTHE_WINDUP_FRACTION = 0.18f
private const val SCYTHE_COCK_DEGREES = 40f
private const val SCYTHE_SWING_DEGREES = 46f

private val scytheGripX = 20.dp
private val scytheGripY = 150.dp
private val scytheGripReach = 108.dp
private val scytheHoldLead = 8.dp

internal const val NECROMANCER_REAP_REACTION_DELAY_MILLIS: Long =
    SCYTHE_SUMMON_MILLIS.toLong() +
        SCYTHE_GLOW_HOLD_MILLIS +
        SCYTHE_RISE_MILLIS.toLong() +
        SCYTHE_REAP_CONTACT_MILLIS

@Composable
internal fun NecromancerNormalAttackOverlay(
    animatingMessage: ArenaMessageState.NormalRoll,
    attackerBounds: Rect,
    defenderBounds: Rect,
    scythePalette: ScythePalette,
    modifier: Modifier = Modifier,
) {
    val necromancerStrikeState = remember { NecromancerStrikeState() }
    val density = LocalDensity.current
    val onAvatarAnimationComplete = ArenaActionsProvider.current.onAvatarComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val messageType = animatingMessage.normalRollMessageType

        necromancerStrikeState.play(
            geometry =
                weaponStrikeGeometry(
                    attackerBounds = attackerBounds,
                    target = defenderBounds.center,
                    accentColor = scythePalette.blade,
                    density = density,
                ),
            palette = scythePalette,
            normalRollMessageType = messageType,
        )

        onAvatarAnimationComplete()
    }

    Canvas(modifier = modifier) {
        drawScytheReap(state = necromancerStrikeState)
    }
}


internal fun necromancerNormalAttackDurationMillis(): Long =
    SCYTHE_SUMMON_MILLIS.toLong() +
        SCYTHE_GLOW_HOLD_MILLIS +
        SCYTHE_RISE_MILLIS.toLong() +
        SCYTHE_REAP_MILLIS.toLong() +
        SCYTHE_REAP_END_HOLD_MILLIS +
        SCYTHE_FADE_MILLIS.toLong()

internal suspend fun NecromancerStrikeState.play(
    geometry: WeaponStrikeGeometry,
    palette: ScythePalette,
    normalRollMessageType: NormalRollMessageType,
) {
    bodyAlpha.snapTo(targetValue = 0f)
    glow.snapTo(targetValue = 0f)
    rise.snapTo(targetValue = 0f)
    reap.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry
    this.palette = palette

    val connects = normalRollMessageType != NormalRollMessageType.ArmorClassMiss
    val burstScale =
        when (normalRollMessageType) {
            NormalRollMessageType.Knockout -> 1.3f
            NormalRollMessageType.Damage -> 1f
            NormalRollMessageType.ArmorClassHit -> 0.8f
            NormalRollMessageType.ArmorClassMiss -> 0f
        }

    coroutineScope {
        launch {
            bodyAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SCYTHE_SUMMON_MILLIS),
            )
        }
        launch {
            glow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SCYTHE_SUMMON_MILLIS),
            )
        }
    }
    delay(timeMillis = SCYTHE_GLOW_HOLD_MILLIS)

    rise.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = SCYTHE_RISE_MILLIS, easing = FastOutSlowInEasing),
    )

    coroutineScope {
        launch {
            reap.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = SCYTHE_REAP_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            if (connects) {
                delay(timeMillis = SCYTHE_REAP_CONTACT_MILLIS)
                impactBurst(
                    impactAlpha = impactAlpha,
                    impactScale = impactScale,
                    peakScale = burstScale,
                )
            }
        }
    }

    delay(timeMillis = SCYTHE_REAP_END_HOLD_MILLIS)
    coroutineScope {
        launch {
            bodyAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SCYTHE_FADE_MILLIS),
            )
        }
        launch {
            glow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SCYTHE_FADE_MILLIS),
            )
        }
    }

    this.scene = null
}

internal fun DrawScope.drawScytheReap(state: NecromancerStrikeState) {
    val geometry = state.scene ?: return
    drawReapSweep(
        geometry = geometry,
        palette = state.palette,
        rise = state.rise.value,
        reap = state.reap.value,
        glow = state.glow.value,
        alpha = state.bodyAlpha.value,
    )

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

private fun DrawScope.drawReapSweep(
    geometry: WeaponStrikeGeometry,
    palette: ScythePalette,
    rise: Float,
    reap: Float,
    glow: Float,
    alpha: Float,
) {
    if (alpha <= 0f) return

    val axisVector = geometry.target - geometry.start
    val axisLength = axisVector.getDistance()
    if (axisLength == 0f) return
    val axis = axisVector / axisLength

    val uprightDegrees = atan2(y = axis.x, x = -axis.y) * 180f / PI.toFloat()
    val grip = Offset(x = scytheGripX.toPx(), y = scytheGripY.toPx())

    val gripHold = geometry.start + axis * scytheHoldLead.toPx()
    val gripStrike = geometry.target - axis * scytheGripReach.toPx()
    val hasReaped = reap > 0f
    val pivot =
        if (hasReaped) gripStrike else lerp(start = gripHold, stop = gripStrike, fraction = rise)

    if (hasReaped) {
        drawSlashArc(
            pivot = pivot,
            palette = palette,
            uprightDegrees = uprightDegrees,
            grip = grip,
            reap = reap,
            alpha = alpha,
        )
    }

    rotate(degrees = uprightDegrees + scytheSwingDegrees(reap = reap), pivot = pivot) {
        translate(left = pivot.x - grip.x, top = pivot.y - grip.y) {
            drawScytheShape(palette = palette, glow = glow, alpha = alpha)
        }
    }
}

private fun scytheSwingDegrees(reap: Float): Float =
    if (reap < SCYTHE_WINDUP_FRACTION) {
        SCYTHE_COCK_DEGREES * (reap / SCYTHE_WINDUP_FRACTION)
    } else {
        val cutProgress = (reap - SCYTHE_WINDUP_FRACTION) / (1f - SCYTHE_WINDUP_FRACTION)
        SCYTHE_COCK_DEGREES - (SCYTHE_COCK_DEGREES + SCYTHE_SWING_DEGREES) * cutProgress
    }

private fun DrawScope.drawSlashArc(
    pivot: Offset,
    palette: ScythePalette,
    uprightDegrees: Float,
    grip: Offset,
    reap: Float,
    alpha: Float,
) {
    val trailStart = maxOf(SCYTHE_WINDUP_FRACTION, reap - SCYTHE_SLASH_TRAIL_SPAN)
    if (reap <= trailStart) return

    val samples = 14
    val arc =
        Path().apply {
            for (sample in 0..samples) {
                val fraction = trailStart + (reap - trailStart) * sample / samples
                val point =
                    bladeTip(
                        pivot = pivot,
                        uprightDegrees = uprightDegrees,
                        grip = grip,
                        reap = fraction,
                    )
                if (sample == 0) {
                    moveTo(x = point.x, y = point.y)
                } else {
                    lineTo(x = point.x, y = point.y)
                }
            }
        }
    val tail =
        bladeTip(pivot = pivot, uprightDegrees = uprightDegrees, grip = grip, reap = trailStart)
    val head = bladeTip(pivot = pivot, uprightDegrees = uprightDegrees, grip = grip, reap = reap)

    drawPath(
        path = arc,
        color = palette.glow,
        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.3f,
    )
    drawPath(
        path = arc,
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.glow, palette.core),
                start = tail,
                end = head,
            ),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        alpha = alpha * 0.85f,
    )
}

private fun bladeTip(pivot: Offset, uprightDegrees: Float, grip: Offset, reap: Float): Offset {
    val radians = (uprightDegrees + scytheSwingDegrees(reap = reap)) * PI.toFloat() / 180f
    val fromGrip = Offset(x = -grip.x, y = -grip.y)
    return Offset(
        x = pivot.x + fromGrip.x * cos(x = radians) - fromGrip.y * sin(x = radians),
        y = pivot.y + fromGrip.x * sin(x = radians) + fromGrip.y * cos(x = radians),
    )
}

@Stable
internal class NecromancerStrikeState {
    val bodyAlpha = Animatable(initialValue = 0f)
    val glow = Animatable(initialValue = 0f)
    val rise = Animatable(initialValue = 0f)
    val reap = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
    var palette by mutableStateOf(value = necroScythePalette)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerNormalAttackKnockoutPreview() {
    NecromancerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerNormalAttackDamagePreview() {
    NecromancerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerNormalAttackArmorClassHitPreview() {
    NecromancerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerNormalAttackArmorClassMissPreview() {
    NecromancerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun NecromancerNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "necromancer-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Necromancer,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Necromancer, seats = seats) {
        NecromancerNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
