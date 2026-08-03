package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.IMPACT_BURST_TAIL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.NormalAttackOverlay
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.slashTrailPath
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.smoothstep
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelBright
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.weaponStrikeGeometry
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val SLASH_SWEEP_MILLIS = 300
internal const val TWIN_SLASH_STAGGER_MILLIS = 500
internal const val TWIN_SLASH_RAISE_MILLIS = 180
internal const val TWIN_SLASH_READY_HOLD_MILLIS = 220
private const val SLASH_CONNECT_MILLIS = 240L
private const val SLASH_FADE_MILLIS = 200
private const val SLASH_END_HOLD_MILLIS = 120L

private const val SLASH_ENGAGE_FRACTION = 0.22f
private const val SWORD_ERECT_DEGREES = 180f

/** When the follow blade lands, so the shared defender reaction (SS-191) meets its cut. */
internal const val TWIN_SWORD_REACTION_DELAY_MILLIS =
    TWIN_SLASH_RAISE_MILLIS +
        TWIN_SLASH_READY_HOLD_MILLIS +
        TWIN_SLASH_STAGGER_MILLIS +
        SLASH_CONNECT_MILLIS

/**
 * One blade of the Duelist's twin-sword slash (SS-194): [sweep] runs 0-to-1 along the blade's arc
 * across the defender, and [alpha] fades the blade in as the cut begins and out once it lands.
 */
@Stable
internal class BladeSlashState {
    val sweep = Animatable(initialValue = 0f)
    val alpha = Animatable(initialValue = 0f)
}

/**
 * The flight of the Duelist's twin-sword slash. The choreography drives these Animatables and the
 * draw layer reads them, so a strike redraws the canvas without recomposing anything.
 */
@Stable
internal class DuelistStrikeState {
    val leadSlash = BladeSlashState()
    val followSlash = BladeSlashState()
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
}

/**
 * The Duelist's normal attack (SS-194): paired blades slashing the defender one after the other.
 * Both blades rise into an erect ready pose at the Duelist's sides and hold, then the lead blade
 * slashes across to the defender and roughly half a second later the follow blade cuts from the
 * other side. Geometry comes from the measured avatar bounds via [avatarBoundsFor]. Sits out any
 * cue that is not a Duelist normal attack.
 */
@Composable
internal fun DuelistNormalAttackOverlay(
    animatingMessage: ArenaMessageState?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { DuelistStrikeState() }
    val density = LocalDensity.current

    NormalAttackOverlay(
        animatingMessage = animatingMessage,
        attackerClassType = CharacterClassType.Duelist,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = { state.scene = null },
        play = { cue, attackerBounds, defenderBounds ->
            state.play(
                geometry =
                    weaponStrikeGeometry(
                        attackerBounds = attackerBounds,
                        target = defenderBounds.center,
                        accentColor = cue.attackerClassType.toColor(),
                        density = density,
                    ),
                normalRollMessageType = cue.normalRollMessageType,
            )
        },
        modifier = modifier,
        draw = { drawTwinSwordSlash(state = state) },
    )
}

/**
 * How long the Duelist's twin-sword combo runs, so the message sequencer can hold the event until
 * both cuts land instead of cutting the combo off (SS-194 / SS-201). This is the one strike that
 * runs longer than the flash window, so it actually extends its message's dwell.
 */
internal fun duelistNormalAttackDurationMillis(): Long {
    val followSweepMillis =
        maxOf(SLASH_SWEEP_MILLIS.toLong(), SLASH_CONNECT_MILLIS + IMPACT_BURST_TAIL_MILLIS)
    return (TWIN_SLASH_RAISE_MILLIS + TWIN_SLASH_READY_HOLD_MILLIS + TWIN_SLASH_STAGGER_MILLIS)
        .toLong() + followSweepMillis + SLASH_END_HOLD_MILLIS + SLASH_FADE_MILLIS
}

internal suspend fun DuelistStrikeState.play(
    geometry: WeaponStrikeGeometry,
    normalRollMessageType: NormalRollMessageType,
) {
    leadSlash.sweep.snapTo(targetValue = 0f)
    leadSlash.alpha.snapTo(targetValue = 0f)
    followSlash.sweep.snapTo(targetValue = 0f)
    followSlash.alpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry

    val connects = normalRollMessageType != NormalRollMessageType.ArmorClassMiss
    val followBurst =
        when (normalRollMessageType) {
            NormalRollMessageType.Knockout -> 1.3f
            NormalRollMessageType.Damage -> 1f
            NormalRollMessageType.ArmorClassHit -> 0.8f
            NormalRollMessageType.ArmorClassMiss -> 0f
        }

    coroutineScope {
        launch { leadSlash.raise() }
        launch { followSlash.raise() }
    }
    delay(timeMillis = TWIN_SLASH_READY_HOLD_MILLIS.toLong())

    coroutineScope {
        launch {
            sweepBlade(blade = leadSlash, connects = connects, burstScale = followBurst * 0.7f)
        }
        launch {
            delay(timeMillis = TWIN_SLASH_STAGGER_MILLIS.toLong())
            sweepBlade(blade = followSlash, connects = connects, burstScale = followBurst)
        }
    }

    delay(timeMillis = SLASH_END_HOLD_MILLIS)
    coroutineScope {
        launch {
            leadSlash.alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_FADE_MILLIS),
            )
        }
        launch {
            followSlash.alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SLASH_FADE_MILLIS),
            )
        }
    }

    this.scene = null
}

/** The blade fades into its erect ready pose, held upright at the Duelist's side before the cut. */
private suspend fun BladeSlashState.raise() {
    sweep.snapTo(targetValue = 0f)
    alpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = TWIN_SLASH_RAISE_MILLIS),
    )
}

/** One blade's cut: sweep the arc from the ready pose to the defender, flaring on contact. */
private suspend fun DuelistStrikeState.sweepBlade(
    blade: BladeSlashState,
    connects: Boolean,
    burstScale: Float,
) {
    coroutineScope {
        launch {
            blade.sweep.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = SLASH_SWEEP_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            if (connects) {
                delay(timeMillis = SLASH_CONNECT_MILLIS)
                impactBurst(
                    impactAlpha = impactAlpha,
                    impactScale = impactScale,
                    peakScale = burstScale,
                )
            }
        }
    }
}

/**
 * The Duelist's twin blades (SS-194): the lead blade slashes in from the defender's left, the
 * follow blade from the right, so the two cuts cross over the defender.
 */
internal fun DrawScope.drawTwinSwordSlash(state: DuelistStrikeState) {
    val geometry = state.scene ?: return

    drawBladeSlash(geometry = geometry, blade = state.leadSlash, fromSide = -1f)
    drawBladeSlash(geometry = geometry, blade = state.followSlash, fromSide = 1f)

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

/**
 * One slashing blade (SS-194): it launches from the attacker's hand — offset to their left or right
 * so the two swords come from either side — and sweeps a bowed arc across to the defender, crossing
 * the attack line so the paired cuts form an X. A class-color trail brightens to steel at the
 * leading edge, with the sword riding the tip of the cut, oriented along the sweep.
 */
private fun DrawScope.drawBladeSlash(
    geometry: WeaponStrikeGeometry,
    blade: BladeSlashState,
    fromSide: Float,
) {
    val alpha = blade.alpha.value
    if (alpha <= 0f) return

    val axisVector = geometry.target - geometry.start
    val axisLength = axisVector.getDistance()
    if (axisLength == 0f) return
    val axis = axisVector / axisLength
    val perpAxis = Offset(x = -axis.y, y = axis.x)

    val start = geometry.start + perpAxis * (fromSide * 26.dp.toPx())
    val end = geometry.target - perpAxis * (fromSide * 22.dp.toPx())
    val bowNormal = perpAxis * fromSide
    val bow = 26.dp.toPx()

    val sweep = blade.sweep.value
    val engage = smoothstep(edge0 = 0f, edge1 = SLASH_ENGAGE_FRACTION, value = sweep)
    val erectTip = start + Offset(x = 0f, y = -64.dp.toPx())
    val arcPoint =
        slashPoint(start = start, end = end, perp = bowNormal, bow = bow, fraction = sweep)
    val position = lerp(start = erectTip, stop = arcPoint, fraction = engage)

    if (engage > 0f) {
        drawPath(
            path =
                slashTrailPath(
                    start = start,
                    end = end,
                    perp = bowNormal,
                    bow = bow,
                    progress = sweep,
                ),
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            geometry.accentColor.copy(alpha = 0.55f),
                            steelBright,
                        ),
                    start = start,
                    end = arcPoint,
                ),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            alpha = alpha * 0.8f * engage,
        )
    }

    val tangent =
        slashTangent(start = start, end = end, perp = bowNormal, bow = bow, fraction = sweep)
    val slashDegrees = atan2(y = tangent.y, x = tangent.x) * 180f / PI.toFloat() - 90f
    val degrees =
        lerpAngle(fromDegrees = SWORD_ERECT_DEGREES, toDegrees = slashDegrees, fraction = engage)
    rotate(degrees = degrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawSwordShape(accentColor = geometry.accentColor, alpha = alpha)
        }
    }
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistNormalAttackKnockoutPreview() {
    DuelistNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistNormalAttackDamagePreview() {
    DuelistNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistNormalAttackArmorClassHitPreview() {
    DuelistNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun DuelistNormalAttackArmorClassMissPreview() {
    DuelistNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun DuelistNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "duelist-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Duelist,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Duelist, seats = seats) {
        DuelistNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
