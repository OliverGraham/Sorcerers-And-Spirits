package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.MinionType
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.drawImpactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.impactBurst
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.lerpAngle
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelBright
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.steelShade
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val LONGSWORD_RAISE_MILLIS = 150
private const val LONGSWORD_SLASH_MILLIS = 150
private const val LONGSWORD_SLASH_CONNECT_MILLIS = 70L
private const val LONGSWORD_END_HOLD_MILLIS = 90L
private const val LONGSWORD_FADE_MILLIS = 180
private const val LONGSWORD_GLINT_FADE_MILLIS = 200

private const val LONGSWORD_BLADE_LENGTH_FRACTION = 0.4f
private const val LONGSWORD_PIVOT_BACK_FRACTION = 0.85f
private const val LONGSWORD_SWING_ARC_DEGREES = 75f
private const val LONGSWORD_SLASH_TILT_DEGREES = 15f
private const val DEGREES_PER_RADIAN = 180f / PI.toFloat()

internal const val SKELETON_LONGSWORD_REACTION_DELAY_MILLIS =
    LONGSWORD_RAISE_MILLIS + LONGSWORD_SLASH_CONNECT_MILLIS

@Composable
internal fun SkeletonLongSwordOverlay(
    animatingMessage: ArenaMessageState.NormalRoll.Attack,
    attackerBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val strikeState = remember { LongSwordStrikeState() }
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        strikeState.play(
            geometry =
            longSwordGeometry(
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                messageType = animatingMessage.normalRollMessageType,
                accentColor = animatingMessage.accentColor,
            ),
            messageType = animatingMessage.normalRollMessageType,
        )

        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = animatingMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    Canvas(modifier = modifier) { drawLongSwordSlash(state = strikeState) }
}

@Immutable
private data class LongSwordGeometry(
    val pivot: Offset,
    val reach: Float,
    val startAngleDegrees: Float,
    val endAngleDegrees: Float,
    val impactCenter: Offset,
    val accentColor: Color,
    val connects: Boolean,
)

private fun longSwordGeometry(
    attackerBounds: Rect,
    defenderBounds: Rect,
    messageType: NormalRollMessageType,
    accentColor: Color,
): LongSwordGeometry {
    val connects = messageType != NormalRollMessageType.ArmorClassMiss

    val cutCenter =
        if (connects) {
            defenderBounds.center
        } else {
            defenderBounds.center + Offset(x = -defenderBounds.width * 0.9f, y = 0f)
        }

    val toDefenderRaw = cutCenter - attackerBounds.center
    val toDefender = toDefenderRaw / toDefenderRaw.getDistance()
    val reach = defenderBounds.height * LONGSWORD_BLADE_LENGTH_FRACTION
    val pivot = cutCenter - toDefender * (reach * LONGSWORD_PIVOT_BACK_FRACTION)
    val swingCenter = angleDegrees(from = pivot, to = cutCenter) + LONGSWORD_SLASH_TILT_DEGREES

    return LongSwordGeometry(
        pivot = pivot,
        reach = reach,
        startAngleDegrees = swingCenter - LONGSWORD_SWING_ARC_DEGREES / 2f,
        endAngleDegrees = swingCenter + LONGSWORD_SWING_ARC_DEGREES / 2f,
        impactCenter = defenderBounds.center,
        accentColor = accentColor,
        connects = connects,
    )
}

private fun angleDegrees(from: Offset, to: Offset): Float {
    val delta = to - from
    return atan2(y = delta.y, x = delta.x) * DEGREES_PER_RADIAN
}

private fun tipAt(pivot: Offset, reach: Float, angleDegrees: Float): Offset {
    val radians = angleDegrees / DEGREES_PER_RADIAN
    return pivot + Offset(x = cos(radians), y = sin(radians)) * reach
}

private suspend fun LongSwordStrikeState.play(
    geometry: LongSwordGeometry,
    messageType: NormalRollMessageType,
) {
    slash.snapTo(targetValue = 0f)
    alpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    glint.snapTo(targetValue = 0f)
    scene = geometry

    val burstScale =
        when (messageType) {
            NormalRollMessageType.Knockout -> 1.3f
            NormalRollMessageType.Damage -> 1f
            NormalRollMessageType.ArmorClassHit -> 0.75f
            NormalRollMessageType.ArmorClassMiss -> 0f
        }

    alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = LONGSWORD_RAISE_MILLIS))

    coroutineScope {
        launch {
            slash.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = LONGSWORD_SLASH_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        launch {
            delay(timeMillis = LONGSWORD_SLASH_CONNECT_MILLIS)
            coroutineScope {
                launch {
                    glint.snapTo(targetValue = 1f)
                    glint.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = LONGSWORD_GLINT_FADE_MILLIS))
                }
                launch {
                    if (geometry.connects) {
                        impactBurst(impactAlpha = impactAlpha, impactScale = impactScale, peakScale = burstScale)
                    }
                }
            }
        }
    }

    delay(timeMillis = LONGSWORD_END_HOLD_MILLIS)
    alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = LONGSWORD_FADE_MILLIS))
    scene = null
}

private fun DrawScope.drawLongSwordSlash(state: LongSwordStrikeState) {
    val geometry = state.scene ?: return
    val alpha = state.alpha.value
    if (alpha <= 0f) return

    val angleDegrees =
        lerpAngle(
            fromDegrees = geometry.startAngleDegrees,
            toDegrees = geometry.endAngleDegrees,
            fraction = state.slash.value,
        )
    val tip = tipAt(pivot = geometry.pivot, reach = geometry.reach, angleDegrees = angleDegrees)

    if (state.slash.value > 0f) {
        val trailStart = tipAt(pivot = geometry.pivot, reach = geometry.reach, angleDegrees = geometry.startAngleDegrees)
        drawSlashTrail(start = trailStart, end = tip, accentColor = geometry.accentColor, alpha = alpha)
    }

    rotate(degrees = angleDegrees + 90f, pivot = geometry.pivot) {
        translate(left = geometry.pivot.x, top = geometry.pivot.y) {
            drawLongSword(
                reach = geometry.reach,
                accentColor = geometry.accentColor,
                alpha = alpha,
                glint = state.glint.value,
            )
        }
    }

    val isBursting = state.impactAlpha.value > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(
            center = geometry.impactCenter,
            scale = state.impactScale.value,
            alpha = state.impactAlpha.value,
            accentColor = geometry.accentColor,
        )
    }
}

private fun DrawScope.drawSlashTrail(start: Offset, end: Offset, accentColor: Color, alpha: Float) {
    drawLine(
        color = accentColor,
        start = start,
        end = end,
        strokeWidth = 22.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.3f,
    )
    drawLine(
        brush =
        Brush.linearGradient(
            colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.55f), steelBright),
            start = start,
            end = end,
        ),
        start = start,
        end = end,
        strokeWidth = 9.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.95f,
    )
    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = end, alpha = alpha * 0.9f)
}

private fun DrawScope.drawLongSword(reach: Float, accentColor: Color, alpha: Float, glint: Float) {
    val gripHalfLength = 10.dp.toPx()
    val gripHalfWidth = 3.5.dp.toPx()
    val guardY = -gripHalfLength
    val tipY = -reach
    val bladeHalfWidth = 5.dp.toPx()
    val taperStart = tipY + (guardY - tipY) * 0.16f
    val crossHalfWidth = 16.dp.toPx()
    val pommelY = gripHalfLength + 3.dp.toPx()

    listOf(18.dp.toPx() to 0.16f, 11.dp.toPx() to 0.3f).forEach { (glowWidth, glowAlpha) ->
        drawLine(
            color = accentColor,
            start = Offset(x = 0f, y = guardY),
            end = Offset(x = 0f, y = tipY),
            strokeWidth = glowWidth,
            cap = StrokeCap.Round,
            alpha = alpha * glowAlpha,
        )
    }

    val blade =
        Path().apply {
            moveTo(x = 0f, y = tipY)
            lineTo(x = -bladeHalfWidth, y = taperStart)
            lineTo(x = -bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = guardY)
            lineTo(x = bladeHalfWidth, y = taperStart)
            close()
        }
    drawPath(
        path = blade,
        brush =
        Brush.linearGradient(
            colors = listOf(Color.White, steelBright, steelShade),
            start = Offset(x = 0f, y = tipY),
            end = Offset(x = 0f, y = guardY),
        ),
        alpha = alpha,
    )

    drawLine(
        color = Color.White,
        start = Offset(x = 0f, y = taperStart),
        end = Offset(x = 0f, y = guardY),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.8f,
    )

    if (glint > 0f) {
        drawLine(
            color = Color.White,
            start = Offset(x = 0f, y = tipY),
            end = Offset(x = 0f, y = guardY),
            strokeWidth = 11.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * glint * 0.4f,
        )
        drawLine(
            color = Color.White,
            start = Offset(x = 0f, y = tipY),
            end = Offset(x = 0f, y = guardY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha * glint,
        )
    }

    drawCircle(
        color = Color.White,
        radius = 2.5.dp.toPx(),
        center = Offset(x = 0f, y = tipY),
        alpha = alpha * 0.9f,
    )

    drawLine(
        color = steelShade,
        start = Offset(x = -crossHalfWidth, y = guardY),
        end = Offset(x = crossHalfWidth, y = guardY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha,
    )
    drawLine(
        color = accentColor,
        start = Offset(x = -crossHalfWidth, y = guardY),
        end = Offset(x = crossHalfWidth, y = guardY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.9f,
    )

    drawRoundRect(
        color = steelShade,
        topLeft = Offset(x = -gripHalfWidth, y = guardY),
        size = Size(width = gripHalfWidth * 2, height = gripHalfLength * 2),
        cornerRadius = CornerRadius(x = gripHalfWidth),
        alpha = alpha,
    )

    drawCircle(color = steelBright, radius = 4.dp.toPx(), center = Offset(x = 0f, y = pommelY), alpha = alpha)
    drawCircle(
        color = accentColor,
        radius = 4.dp.toPx(),
        center = Offset(x = 0f, y = pommelY),
        alpha = alpha * 0.5f,
    )
}

@Stable
private class LongSwordStrikeState {
    val slash = Animatable(initialValue = 0f)
    val alpha = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)
    val glint = Animatable(initialValue = 0f)

    var scene by mutableStateOf<LongSwordGeometry?>(value = null)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun SkeletonLongSwordKnockoutPreview() {
    SkeletonLongSwordPreview(
        normalRollMessageType = NormalRollMessageType.Knockout,
        accentColor = CharacterClassType.Necromancer.toColor(),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun SkeletonLongSwordDamagePreview() {
    SkeletonLongSwordPreview(
        normalRollMessageType = NormalRollMessageType.Damage,
        accentColor = CharacterClassType.Illusionist.toColor(),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun SkeletonLongSwordMissPreview() {
    SkeletonLongSwordPreview(
        normalRollMessageType = NormalRollMessageType.ArmorClassMiss,
        accentColor = CharacterClassType.Necromancer.toColor(),
    )
}

@Composable
private fun SkeletonLongSwordPreview(normalRollMessageType: NormalRollMessageType, accentColor: Color) {
    val avatars = rememberDuoAvatars()
    AnimationPreview(
        accentClass = CharacterClassType.Necromancer,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.NormalRoll.Attack(
                id = id,
                attackerUserId = PREVIEW_ACTOR_ID,
                defenderUserId = PREVIEW_TARGET_ID,
                normalRollMessageType = normalRollMessageType,
                impersonatedClassType = CharacterClassType.None,
                minionAttackerIndex = 0,
                minionAttackerType = MinionType.Skeleton,
                animation = ArenaAnimationState.Avatar(),
                gestures = emptyMap(),
                accentColor = accentColor,
                weaponType = WeaponType.LongSword,
            )
        },
    ) { message ->
        SkeletonLongSwordOverlay(
            animatingMessage = message,
            attackerBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            defenderBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
