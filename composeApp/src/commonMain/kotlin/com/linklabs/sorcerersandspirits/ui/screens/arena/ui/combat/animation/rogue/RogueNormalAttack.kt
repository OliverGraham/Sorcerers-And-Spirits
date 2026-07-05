package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AbilityAnimationStage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.NormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.projectileStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoSeats
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberReplayingCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelBright
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelShade
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val MELEE_DASH_MILLIS = 200
internal const val MELEE_THRUST_MILLIS = 100
internal const val MELEE_EMBED_MILLIS = 160L
internal const val MELEE_REACTION_DELAY_MILLIS = 280L

private const val WITHDRAW_MILLIS = 220

/**
 * The flight of the Rogue's dagger strike. The choreography drives these Animatables and the draw
 * layer reads them, so a strike redraws the canvas without recomposing anything.
 */
@Stable
internal class RogueStrikeState {
    val travel = Animatable(initialValue = 0f)
    val weaponAlpha = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
}

/**
 * The Rogue's normal attack (SS-192): a close-combat dagger strike. The dagger appears in front of
 * the avatar as it dashes into range, thrusts into the defender avatar — or past it on a miss —
 * flares a burst in the Rogue's class color, and withdraws as the avatar settles back. Geometry
 * comes from the measured avatar bounds via [avatarBoundsFor], so the pierce always lands on the
 * defender art regardless of layout. Sits out any cue that is not a Rogue normal attack.
 */
@Composable
internal fun RogueNormalAttackOverlay(
    animationCue: CombatAnimationCue?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { RogueStrikeState() }
    val density = LocalDensity.current

    NormalAttackOverlay(
        animationCue = animationCue,
        attackerClassType = CharacterClassType.Rogue,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = { state.scene = null },
        play = { cue, attackerBounds, defenderBounds ->
            state.play(
                geometry =
                    projectileStrikeGeometry(
                        cue = cue,
                        attackerBounds = attackerBounds,
                        defenderBounds = defenderBounds,
                        density = density,
                    ),
                outcome = cue.outcome,
            )
        },
        modifier = modifier,
        draw = { drawRogueStrike(state = state) },
    )
}

/**
 * How long the Rogue's dagger strike runs, so the message sequencer can hold the event until its
 * strike finishes instead of cutting it off (SS-201). A strike that fits inside the flash window
 * does not change pacing; a knockout drives the blade a touch deeper and runs slightly longer.
 */
internal fun rogueNormalAttackDurationMillis(outcome: NormalRollMessageType): Long =
    MELEE_DASH_MILLIS +
        MELEE_THRUST_MILLIS +
        MELEE_EMBED_MILLIS +
        WITHDRAW_MILLIS +
        if (outcome == NormalRollMessageType.Knockout) 120L else 0L

private suspend fun RogueStrikeState.play(
    geometry: WeaponStrikeGeometry,
    outcome: NormalRollMessageType,
) {
    travel.snapTo(targetValue = 0f)
    weaponAlpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry

    when (outcome) {
        NormalRollMessageType.ArmorClassHit ->
            pierce(dashFraction = geometry.dashFraction, burstScale = 0.8f)

        NormalRollMessageType.ArmorClassMiss -> whiff(dashFraction = geometry.dashFraction)

        NormalRollMessageType.Damage ->
            pierce(dashFraction = geometry.dashFraction, burstScale = 1f)

        NormalRollMessageType.Knockout -> pierceDeep(dashFraction = geometry.dashFraction)
    }

    this.scene = null
}

/** The blade fades in at the attacker's hands and rides the avatar's dash into melee range. */
private suspend fun RogueStrikeState.closeIn(dashFraction: Float) {
    coroutineScope {
        launch {
            weaponAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 120))
        }
        launch {
            travel.animateTo(
                targetValue = dashFraction,
                animationSpec = tween(durationMillis = MELEE_DASH_MILLIS),
            )
        }
    }
}

/**
 * The close-combat pierce: dash into range, thrust the blade into the defender, hold it embedded
 * while the burst flares, then withdraw as the attacker settles back.
 */
private suspend fun RogueStrikeState.pierce(dashFraction: Float, burstScale: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS, easing = FastOutLinearInEasing),
    )
    coroutineScope {
        launch {
            impactBurst(
                impactAlpha = impactAlpha,
                impactScale = impactScale,
                peakScale = burstScale,
            )
        }
        launch {
            delay(timeMillis = MELEE_EMBED_MILLIS)
            withdraw(dashFraction = dashFraction)
        }
    }
}

/**
 * The knockout: the pierce, then the blade driven deeper with a bigger burst before withdrawing.
 */
private suspend fun RogueStrikeState.pierceDeep(dashFraction: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MELEE_THRUST_MILLIS, easing = FastOutLinearInEasing),
    )
    coroutineScope {
        launch {
            impactAlpha.snapTo(targetValue = 1f)
            impactScale.snapTo(targetValue = 0f)
            impactScale.animateTo(targetValue = 0.7f, animationSpec = tween(durationMillis = 100))
            impactAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 100))
        }
        launch {
            delay(timeMillis = 80L)
            travel.animateTo(targetValue = 1.1f, animationSpec = tween(durationMillis = 120))
        }
    }
    coroutineScope {
        launch {
            impactBurst(impactAlpha = impactAlpha, impactScale = impactScale, peakScale = 1.3f)
        }
        launch {
            delay(timeMillis = MELEE_EMBED_MILLIS)
            withdraw(dashFraction = dashFraction)
        }
    }
}

/** The miss: the thrust carries past the defender into empty air, hangs, and is pulled back. */
private suspend fun RogueStrikeState.whiff(dashFraction: Float) {
    closeIn(dashFraction = dashFraction)
    travel.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
    )
    delay(timeMillis = 120L)
    withdraw(dashFraction = dashFraction)
}

private suspend fun RogueStrikeState.withdraw(dashFraction: Float) {
    coroutineScope {
        launch {
            travel.animateTo(
                targetValue = 1f - (1f - dashFraction) * 0.6f,
                animationSpec = tween(durationMillis = WITHDRAW_MILLIS),
            )
        }
        launch {
            weaponAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = WITHDRAW_MILLIS),
            )
        }
    }
}

private fun DrawScope.drawRogueStrike(state: RogueStrikeState) {
    val geometry = state.scene ?: return

    val weaponAlpha = state.weaponAlpha.value
    if (weaponAlpha > 0f) {
        val position =
            lerp(start = geometry.start, stop = geometry.target, fraction = state.travel.value)
        drawBladeThrust(
            geometry = geometry,
            travel = state.travel.value,
            position = position,
            alpha = weaponAlpha,
        )
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

/** The flash of steel: a class-color streak trailing the blade, brightening to white at its tip. */
private fun DrawScope.drawBladeThrust(
    geometry: WeaponStrikeGeometry,
    travel: Float,
    position: Offset,
    alpha: Float,
) {
    val trailStart =
        lerp(
            start = geometry.start,
            stop = geometry.target,
            fraction = (travel - 0.18f).coerceAtLeast(minimumValue = 0f),
        )

    drawLine(
        brush =
            Brush.linearGradient(
                colors =
                    listOf(Color.Transparent, geometry.accentColor.copy(alpha = 0.6f), steelBright),
                start = trailStart,
                end = position,
            ),
        start = trailStart,
        end = position,
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.7f,
    )

    drawDagger(geometry = geometry, position = position, alpha = alpha)
}

private fun DrawScope.drawDagger(geometry: WeaponStrikeGeometry, position: Offset, alpha: Float) {
    val thrustVector = geometry.target - geometry.start
    val degreesToTarget = atan2(y = thrustVector.y, x = thrustVector.x) * 180f / PI.toFloat() - 90f

    rotate(degrees = degreesToTarget, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawDaggerShape(accentColor = geometry.accentColor, alpha = alpha)
        }
    }
}

/**
 * The dagger in local coordinates: tip at the origin leading the thrust, blade trailing behind it,
 * with the guard, hilt, and pommel bringing up the rear. The blade is a steel gradient; the guard
 * and hilt carry the class-color accent.
 */
private fun DrawScope.drawDaggerShape(accentColor: Color, alpha: Float) {
    val bladeLength = 52.dp.toPx()
    val bladeHalfWidth = 6.5.dp.toPx()
    val guardHalfWidth = 13.dp.toPx()
    val guardHeight = 5.dp.toPx()
    val hiltHalfWidth = 3.dp.toPx()
    val hiltLength = 16.dp.toPx()

    val blade =
        Path().apply {
            moveTo(x = 0f, y = 0f)
            lineTo(x = -bladeHalfWidth, y = -bladeLength * 0.85f)
            lineTo(x = -bladeHalfWidth * 0.55f, y = -bladeLength)
            lineTo(x = bladeHalfWidth * 0.55f, y = -bladeLength)
            lineTo(x = bladeHalfWidth, y = -bladeLength * 0.85f)
            close()
        }
    drawPath(
        path = blade,
        brush =
            Brush.linearGradient(
                colors = listOf(steelBright, steelShade),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 0f, y = -bladeLength),
            ),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -guardHalfWidth, y = -bladeLength - guardHeight),
        size = Size(width = guardHalfWidth * 2, height = guardHeight),
        cornerRadius = CornerRadius(x = guardHeight / 2),
        alpha = alpha,
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(x = -hiltHalfWidth, y = -bladeLength - guardHeight - hiltLength),
        size = Size(width = hiltHalfWidth * 2, height = hiltLength),
        cornerRadius = CornerRadius(x = hiltHalfWidth),
        alpha = alpha,
    )

    drawCircle(
        color = steelShade,
        radius = 4.5.dp.toPx(),
        center = Offset(x = 0f, y = -bladeLength - guardHeight - hiltLength - 3.dp.toPx()),
        alpha = alpha,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackKnockoutPreview() {
    RogueNormalAttackPreview(outcome = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackDamagePreview() {
    RogueNormalAttackPreview(outcome = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackArmorClassHitPreview() {
    RogueNormalAttackPreview(outcome = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackArmorClassMissPreview() {
    RogueNormalAttackPreview(outcome = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun RogueNormalAttackPreview(outcome: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = outcome) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "rogue-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Rogue,
                defenderUserId = PREVIEW_TARGET_ID,
                outcome = outcome,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Rogue, seats = seats) {
        RogueNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
