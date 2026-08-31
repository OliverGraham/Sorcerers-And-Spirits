package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_TARGET_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.projectileStrikeGeometry
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelBright
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelShade
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

internal const val MELEE_DASH_MILLIS = 200
internal const val MELEE_THRUST_MILLIS = 100
internal const val MELEE_EMBED_MILLIS = 160L
internal const val MELEE_REACTION_DELAY_MILLIS = 280L

private const val WITHDRAW_MILLIS = 220

@Composable
internal fun RogueNormalAttackOverlay(
    animatingMessage: ArenaMessageState.NormalRoll,
    attackerBounds: Rect,
    defenderBounds: Rect,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val rogueStrikeState = remember { RogueStrikeState() }
    val density = LocalDensity.current
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        val messageType = animatingMessage.normalRollMessageType

        rogueStrikeState.play(
            geometry =
            projectileStrikeGeometry(
                messageType = messageType,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                accentColor = accentColor,
                density = density,
            ),
            normalRollMessageType = messageType,
        )

        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = animatingMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    Canvas(modifier = modifier) { drawRogueStrike(state = rogueStrikeState) }
}

internal suspend fun RogueStrikeState.play(
    geometry: WeaponStrikeGeometry,
    normalRollMessageType: NormalRollMessageType,
) {
    travel.snapTo(targetValue = 0f)
    weaponAlpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    this.scene = geometry

    when (normalRollMessageType) {
        NormalRollMessageType.ArmorClassHit ->
            pierce(dashFraction = geometry.dashFraction, burstScale = 0.8f)

        NormalRollMessageType.ArmorClassMiss -> whiff(dashFraction = geometry.dashFraction)

        NormalRollMessageType.Damage ->
            pierce(dashFraction = geometry.dashFraction, burstScale = 1f)

        NormalRollMessageType.Knockout -> pierceDeep(dashFraction = geometry.dashFraction)
    }

    this.scene = null
}

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

internal fun DrawScope.drawRogueStrike(state: RogueStrikeState) {
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
    RogueNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Knockout)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackDamagePreview() {
    RogueNormalAttackPreview(normalRollMessageType = NormalRollMessageType.Damage)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackArmorClassHitPreview() {
    RogueNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassHit)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun RogueNormalAttackArmorClassMissPreview() {
    RogueNormalAttackPreview(normalRollMessageType = NormalRollMessageType.ArmorClassMiss)
}

@Composable
private fun RogueNormalAttackPreview(normalRollMessageType: NormalRollMessageType) {
    val avatars = rememberDuoAvatars()
    AnimationPreview(
        accentClass = CharacterClassType.Rogue,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.NormalRoll.Attack(
                id = id,
                attackerUserId = PREVIEW_ACTOR_ID,
                attackerClassType = CharacterClassType.Rogue,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
                minionAttackerIndex = null,
                minionAttackerType = null,
                animation = ArenaAnimationState.Avatar(),
                gestures = emptyMap(),
            )
        },
    ) { message ->
        RogueNormalAttackOverlay(
            animatingMessage = message,
            attackerBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            defenderBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            accentColor = CharacterClassType.Rogue.toColor(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
