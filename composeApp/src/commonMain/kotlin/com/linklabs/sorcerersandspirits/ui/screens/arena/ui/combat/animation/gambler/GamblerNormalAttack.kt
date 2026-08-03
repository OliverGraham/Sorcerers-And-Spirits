package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val THROW_REACTION_DELAY_MILLIS = 400L

private const val THROW_FLIGHT_MILLIS = 440
private const val CARD_SPIN_ROTATIONS = 4f

private val cardFace = Color(color = 0xFFFBF6EC)
private val cardEdge = Color(color = 0xFFD8CFBC)

/**
 * The flight of the Gambler's thrown card. The choreography drives these Animatables and the draw
 * layer reads them, so a throw redraws the canvas without recomposing anything.
 */
@Stable
internal class GamblerStrikeState {
    val travel = Animatable(initialValue = 0f)
    val weaponAlpha = Animatable(initialValue = 0f)
    val spin = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<WeaponStrikeGeometry?>(value = null)
}

/**
 * The Gambler's normal attack (SS-193): a thrown playing card. The card spins the full span to the
 * defender and flares a burst on landing, or spins past into empty air on a miss. Nothing withdraws
 * — a thrown card lands and is gone, so the card fades into the burst rather than being pulled
 * back. Geometry comes from the measured avatar bounds via [avatarBoundsFor]. Sits out any cue that
 * is not a Gambler normal attack.
 */
@Composable
internal fun GamblerNormalAttackOverlay(
    animatingMessage: ArenaMessageState?,
    avatarBoundsFor: (String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val state = remember { GamblerStrikeState() }
    val density = LocalDensity.current

    NormalAttackOverlay(
        animatingMessage = animatingMessage,
        attackerClassType = CharacterClassType.Gambler,
        avatarBoundsFor = avatarBoundsFor,
        onCancel = { state.scene = null },
        play = { animation, attackerBounds, defenderBounds ->
            state.play(
                geometry =
                    projectileStrikeGeometry(
                        cue = cue,
                        attackerBounds = attackerBounds,
                        defenderBounds = defenderBounds,
                        accentColor = animatingMessage?.actingClassType?.toColor(),
                        density = density,
                    ),
                normalRollMessageType = cue.normalRollMessageType,
            )
        },
        modifier = modifier,
        draw = { drawGamblerStrike(state = state) },
    )
}

/**
 * How long the Gambler's card throw runs, so the message sequencer can hold the event until the
 * card lands instead of cutting it off (SS-201).
 */
internal fun gamblerNormalAttackDurationMillis(): Long = THROW_FLIGHT_MILLIS + 200L

internal suspend fun GamblerStrikeState.play(
    geometry: WeaponStrikeGeometry,
    normalRollMessageType: NormalRollMessageType,
) {
    travel.snapTo(targetValue = 0f)
    weaponAlpha.snapTo(targetValue = 0f)
    spin.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry

    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit -> throwAcross(burstScale = 0.8f)
        NormalRollMessageType.ArmorClassMiss -> throwPast()
        NormalRollMessageType.Damage -> throwAcross(burstScale = 1f)
        NormalRollMessageType.Knockout -> throwAcross(burstScale = 1.3f)
    }

    this.scene = null
}

/** The card's flight to its target: it appears at the hand and spins across the gap. */
private suspend fun GamblerStrikeState.flyAcross(durationMillis: Int) {
    weaponAlpha.snapTo(targetValue = 1f)
    coroutineScope {
        launch {
            travel.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = durationMillis, easing = FastOutLinearInEasing),
            )
        }
        launch {
            spin.animateTo(
                targetValue = CARD_SPIN_ROTATIONS * 360f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
        }
    }
}

/** A landed throw: the card spins to the defender, then vanishes into a class-color burst. */
private suspend fun GamblerStrikeState.throwAcross(burstScale: Float) {
    flyAcross(durationMillis = THROW_FLIGHT_MILLIS)
    coroutineScope {
        launch {
            impactBurst(
                impactAlpha = impactAlpha,
                impactScale = impactScale,
                peakScale = burstScale,
            )
        }
        launch {
            weaponAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 110))
        }
    }
}

/** The missed throw: the card spins past the sidestepping defender and fades into empty air. */
private suspend fun GamblerStrikeState.throwPast() {
    flyAcross(durationMillis = THROW_FLIGHT_MILLIS)
    coroutineScope {
        launch { travel.animateTo(targetValue = 1.3f, animationSpec = tween(durationMillis = 180)) }
        launch {
            spin.animateTo(
                targetValue = (CARD_SPIN_ROTATIONS + 0.5f) * 360f,
                animationSpec = tween(durationMillis = 180),
            )
        }
        launch {
            weaponAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 180))
        }
    }
}

internal fun DrawScope.drawGamblerStrike(state: GamblerStrikeState) {
    val geometry = state.scene ?: return

    val weaponAlpha = state.weaponAlpha.value
    if (weaponAlpha > 0f) {
        val position =
            lerp(start = geometry.start, stop = geometry.target, fraction = state.travel.value)
        drawCardThrow(
            geometry = geometry,
            position = position,
            spinDegrees = state.spin.value,
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

/**
 * The thrown playing card (SS-193): a soft class-color trail marks its arc, and the card itself
 * spins in-plane about its center as it flies — a spade pip in the class color on a poker-card
 * face.
 */
private fun DrawScope.drawCardThrow(
    geometry: WeaponStrikeGeometry,
    position: Offset,
    spinDegrees: Float,
    alpha: Float,
) {
    drawLine(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, geometry.accentColor.copy(alpha = 0.35f)),
                start = geometry.start,
                end = position,
            ),
        start = geometry.start,
        end = position,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.5f,
    )

    rotate(degrees = spinDegrees, pivot = position) {
        translate(left = position.x, top = position.y) {
            drawPlayingCard(accentColor = geometry.accentColor, alpha = alpha)
        }
    }
}

/**
 * The playing card in local coordinates, centered on the origin so it spins about its own middle: a
 * pale poker-card face rimmed in the class color, with a single spade pip in the class color at its
 * heart.
 */
private fun DrawScope.drawPlayingCard(accentColor: Color, alpha: Float) {
    val halfWidth = 15.dp.toPx()
    val halfHeight = 21.dp.toPx()
    val corner = CornerRadius(x = 3.5.dp.toPx())
    val topLeft = Offset(x = -halfWidth, y = -halfHeight)
    val cardSize = Size(width = halfWidth * 2, height = halfHeight * 2)

    drawRoundRect(
        color = cardFace,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = corner,
        alpha = alpha,
    )
    drawRoundRect(
        color = cardEdge,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = corner,
        style = Stroke(width = 1.5.dp.toPx()),
        alpha = alpha,
    )

    drawPath(
        path = spadePath(halfWidth = 7.dp.toPx(), halfHeight = 9.dp.toPx()),
        color = accentColor,
        alpha = alpha,
    )
}

/**
 * A spade centered on the origin, point up over a short stem: two lobes swept out with mirrored
 * cubics that curl into the base, then a trapezoidal stem closing it off.
 */
private fun spadePath(halfWidth: Float, halfHeight: Float): Path =
    Path().apply {
        moveTo(x = 0f, y = -halfHeight)
        cubicTo(
            x1 = halfWidth * 0.35f,
            y1 = -halfHeight * 0.5f,
            x2 = halfWidth,
            y2 = -halfHeight * 0.05f,
            x3 = halfWidth,
            y3 = halfHeight * 0.35f,
        )
        cubicTo(
            x1 = halfWidth,
            y1 = halfHeight * 0.8f,
            x2 = halfWidth * 0.4f,
            y2 = halfHeight * 0.72f,
            x3 = halfWidth * 0.14f,
            y3 = halfHeight * 0.42f,
        )
        lineTo(x = halfWidth * 0.32f, y = halfHeight)
        lineTo(x = -halfWidth * 0.32f, y = halfHeight)
        lineTo(x = -halfWidth * 0.14f, y = halfHeight * 0.42f)
        cubicTo(
            x1 = -halfWidth * 0.4f,
            y1 = halfHeight * 0.72f,
            x2 = -halfWidth,
            y2 = halfHeight * 0.8f,
            x3 = -halfWidth,
            y3 = halfHeight * 0.35f,
        )
        cubicTo(
            x1 = -halfWidth,
            y1 = -halfHeight * 0.05f,
            x2 = -halfWidth * 0.35f,
            y2 = -halfHeight * 0.5f,
            x3 = 0f,
            y3 = -halfHeight,
        )
        close()
    }

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerNormalAttackKnockoutPreview() {
    GamblerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerNormalAttackDamagePreview() {
    GamblerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerNormalAttackArmorClassHitPreview() {
    GamblerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GamblerNormalAttackArmorClassMissPreview() {
    GamblerNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun GamblerNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val seats = rememberDuoSeats()
    val cue =
        rememberReplayingCue(replayKey = normalRollMessageType) { iteration ->
            CombatAnimationCue.NormalAttack(
                id = "gambler-attack-$iteration",
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Gambler,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
            )
        }
    AbilityAnimationStage(accentClass = CharacterClassType.Gambler, seats = seats) {
        GamblerNormalAttackOverlay(
            animationCue = cue,
            avatarBoundsFor = seats.boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
