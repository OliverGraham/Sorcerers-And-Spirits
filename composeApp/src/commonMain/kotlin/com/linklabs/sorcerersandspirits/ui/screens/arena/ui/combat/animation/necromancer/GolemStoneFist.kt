package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.minionChargeDistance
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.missThrustTarget
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberDuoAvatars
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val STONEFIST_WINDUP_MILLIS = 150
private const val STONEFIST_SLAM_MILLIS = 120
private const val STONEFIST_SHATTER_MILLIS = 420
private const val STONEFIST_END_HOLD_MILLIS = 40L
private const val STONEFIST_MISS_FADE_MILLIS = 200

private const val STONEFIST_RADIUS_FRACTION = 0.3f
private const val STONEFIST_WINDUP_PULL_FRACTION = 0.6f
private const val STONEFIST_SHARD_COUNT = 14
private const val DEGREES_PER_RADIAN = 180f / PI.toFloat()

/** The silvery grey rock the golem's fist is hewn from, matching the golem png. */
private val stoneLight = Color(color = 0xFFCDD2CD)
private val stoneMid = Color(color = 0xFF8B918B)
private val stoneDark = Color(color = 0xFF565B56)

internal const val GOLEM_STONEFIST_REACTION_DELAY_MILLIS =
    (STONEFIST_WINDUP_MILLIS + STONEFIST_SLAM_MILLIS).toLong()

@Composable
internal fun GolemStoneFistOverlay(
    animatingMessage: ArenaMessageState.NormalRoll.Attack,
    attackerBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val strikeState = remember { StoneFistStrikeState() }
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete
    val chargeForwardPx = with(receiver = LocalDensity.current) { minionChargeDistance.toPx() }

    LaunchedEffect(key1 = animatingMessage.id) {
        strikeState.play(
            geometry =
            stoneFistGeometry(
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                displayIndex = animatingMessage.minionAttackerIndex ?: 0,
                minionType = animatingMessage.minionAttackerType ?: MinionType.Golem,
                chargeForwardPx = chargeForwardPx,
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

    Canvas(modifier = modifier) { drawStoneFist(state = strikeState) }
}

@Immutable
private data class StoneFistGeometry(
    val windupPoint: Offset,
    val impactPoint: Offset,
    val aimAngleDegrees: Float,
    val fistRadius: Float,
    val accentColor: Color,
    val connects: Boolean,
    val shards: List<StoneShard>,
)

@Immutable
private data class StoneShard(
    val angleDegrees: Float,
    val distance: Float,
    val size: Float,
    val spinDegrees: Float,
)

private fun stoneFistGeometry(
    attackerBounds: Rect,
    defenderBounds: Rect,
    displayIndex: Int,
    minionType: MinionType,
    chargeForwardPx: Float,
    messageType: NormalRollMessageType,
    accentColor: Color,
): StoneFistGeometry {
    val connects = messageType != NormalRollMessageType.ArmorClassMiss

    val restRect =
        minionRestingSlot(displayIndex = displayIndex, minionType = minionType, area = attackerBounds).toRect()
    val strikeDirection = if (defenderBounds.center.y < attackerBounds.center.y) -1f else 1f
    val fistOrigin =
        Offset(
            x = attackerBounds.center.x,
            y = restRect.center.y + strikeDirection * chargeForwardPx,
        )

    val impactPoint =
        if (connects) {
            defenderBounds.center
        } else {
            missThrustTarget(defenderBounds = defenderBounds, origin = fistOrigin)
        }

    val fistRadius = restRect.height * STONEFIST_RADIUS_FRACTION
    val aimAngleDegrees = angleDegrees(from = fistOrigin, to = impactPoint)
    val aimDirection = unitVector(angleDegrees = aimAngleDegrees)
    val windupPoint = fistOrigin - aimDirection * fistRadius * STONEFIST_WINDUP_PULL_FRACTION

    return StoneFistGeometry(
        windupPoint = windupPoint,
        impactPoint = impactPoint,
        aimAngleDegrees = aimAngleDegrees,
        fistRadius = fistRadius,
        accentColor = accentColor,
        connects = connects,
        shards = stoneShards(fistRadius = fistRadius),
    )
}

private fun stoneShards(fistRadius: Float): List<StoneShard> = (0 until STONEFIST_SHARD_COUNT).map { index ->
    val baseAngle = index * (360f / STONEFIST_SHARD_COUNT)
    val jitter = sin(x = index * 2.399f) * 12f
    StoneShard(
        angleDegrees = baseAngle + jitter,
        distance = fistRadius * (2.1f + 1f * abs(x = sin(x = index * 1.7f))),
        size = fistRadius * (0.22f + 0.16f * abs(x = cos(x = index * 1.3f))),
        spinDegrees = (if (index % 2 == 0) 1f else -1f) * (140f + 90f * abs(x = sin(x = index.toFloat()))),
    )
}

private fun angleDegrees(from: Offset, to: Offset): Float {
    val delta = to - from
    return atan2(y = delta.y, x = delta.x) * DEGREES_PER_RADIAN
}

private fun unitVector(angleDegrees: Float): Offset {
    val radians = angleDegrees / DEGREES_PER_RADIAN
    return Offset(x = cos(x = radians), y = sin(x = radians))
}

private suspend fun StoneFistStrikeState.play(
    geometry: StoneFistGeometry,
    messageType: NormalRollMessageType,
) {
    slam.snapTo(targetValue = 0f)
    shatter.snapTo(targetValue = 0f)
    alpha.snapTo(targetValue = 0f)
    impactAlpha.snapTo(targetValue = 0f)
    impactScale.snapTo(targetValue = 0f)
    scene = geometry

    val burstScale =
        when (messageType) {
            NormalRollMessageType.Knockout -> 1.4f
            NormalRollMessageType.Damage -> 1.05f
            NormalRollMessageType.ArmorClassHit -> 0.8f
            NormalRollMessageType.ArmorClassMiss -> 0f
        }

    alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = STONEFIST_WINDUP_MILLIS))
    slam.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = STONEFIST_SLAM_MILLIS, easing = FastOutLinearInEasing),
    )

    if (geometry.connects) {
        coroutineScope {
            launch { impactBurst(impactAlpha = impactAlpha, impactScale = impactScale, peakScale = burstScale) }
            launch {
                shatter.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = STONEFIST_SHATTER_MILLIS, easing = FastOutSlowInEasing),
                )
            }
            launch {
                alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = STONEFIST_SHATTER_MILLIS / 2))
            }
        }
        delay(timeMillis = STONEFIST_END_HOLD_MILLIS)
    } else {
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = STONEFIST_MISS_FADE_MILLIS))
    }

    scene = null
}

private fun DrawScope.drawStoneFist(state: StoneFistStrikeState) {
    val geometry = state.scene ?: return

    val fistCenter =
        geometry.windupPoint + (geometry.impactPoint - geometry.windupPoint) * state.slam.value
    val fistAlpha = state.alpha.value

    if (fistAlpha > 0f) {
        rotate(degrees = geometry.aimAngleDegrees + 90f, pivot = fistCenter) {
            translate(left = fistCenter.x, top = fistCenter.y) {
                drawFist(radius = geometry.fistRadius, alpha = fistAlpha)
            }
        }
    }

    val isBursting = state.impactAlpha.value > 0f && state.impactScale.value > 0f
    if (isBursting) {
        drawImpactBurst(
            center = geometry.impactPoint,
            scale = state.impactScale.value,
            alpha = state.impactAlpha.value,
            accentColor = geometry.accentColor,
        )
    }

    if (state.shatter.value > 0f) {
        drawShatter(
            center = geometry.impactPoint,
            shards = geometry.shards,
            accentColor = geometry.accentColor,
            progress = state.shatter.value,
        )
    }
}

private fun DrawScope.drawFist(radius: Float, alpha: Float) {
    val knuckleY = -radius
    val wristY = radius

    val body =
        Path().apply {
            moveTo(x = -radius * 0.78f, y = knuckleY + radius * 0.55f)
            lineTo(x = -radius * 0.95f, y = -radius * 0.15f)
            lineTo(x = -radius * 0.62f, y = wristY * 0.72f)
            lineTo(x = radius * 0.6f, y = wristY * 0.72f)
            lineTo(x = radius * 0.95f, y = -radius * 0.15f)
            lineTo(x = radius * 0.78f, y = knuckleY + radius * 0.55f)
            close()
        }
    drawPath(
        path = body,
        brush =
        Brush.linearGradient(
            colors = listOf(stoneLight, stoneMid, stoneDark),
            start = Offset(x = 0f, y = knuckleY),
            end = Offset(x = 0f, y = wristY),
        ),
        alpha = alpha,
    )

    val knuckleRadius = radius * 0.3f
    listOf(-0.62f, -0.2f, 0.22f, 0.64f).forEachIndexed { index, fraction ->
        val knuckleCenter = Offset(x = radius * fraction, y = knuckleY + knuckleRadius * 0.85f)
        drawCircle(
            brush =
            Brush.radialGradient(
                colors = listOf(stoneLight, stoneMid),
                center = knuckleCenter - Offset(x = knuckleRadius * 0.3f, y = knuckleRadius * 0.3f),
                radius = knuckleRadius * 1.4f,
            ),
            radius = knuckleRadius,
            center = knuckleCenter,
            alpha = alpha,
        )
        if (index > 0) {
            drawLine(
                color = stoneDark,
                start = Offset(x = radius * fraction - knuckleRadius, y = knuckleY + radius * 0.05f),
                end = Offset(x = radius * fraction - knuckleRadius, y = radius * 0.45f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = alpha * 0.6f,
            )
        }
    }

    drawPath(
        path = body,
        color = stoneDark,
        style = Stroke(width = 2.5.dp.toPx()),
        alpha = alpha * 0.7f,
    )

    drawLine(
        color = stoneLight,
        start = Offset(x = -radius * 0.5f, y = -radius * 0.1f),
        end = Offset(x = radius * 0.1f, y = radius * 0.45f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha * 0.5f,
    )
}

private fun DrawScope.drawShatter(
    center: Offset,
    shards: List<StoneShard>,
    accentColor: Color,
    progress: Float,
) {
    val fade = (1f - progress) * (1f - progress)
    shards.forEach { shard ->
        val direction = unitVector(angleDegrees = shard.angleDegrees)
        val shardCenter = center + direction * shard.distance * progress
        val shardSize = shard.size * (1f - progress * 0.4f)
        rotate(degrees = shard.spinDegrees * progress, pivot = shardCenter) {
            drawShard(center = shardCenter, size = shardSize, accentColor = accentColor, alpha = fade)
        }
    }
}

private fun DrawScope.drawShard(center: Offset, size: Float, accentColor: Color, alpha: Float) {
    val shard =
        Path().apply {
            moveTo(x = center.x, y = center.y - size)
            lineTo(x = center.x + size * 0.82f, y = center.y - size * 0.08f)
            lineTo(x = center.x + size * 0.3f, y = center.y + size)
            lineTo(x = center.x - size * 0.9f, y = center.y + size * 0.22f)
            close()
        }
    drawPath(
        path = shard,
        brush =
        Brush.linearGradient(
            colors = listOf(accentColor, lerp(start = accentColor, stop = stoneDark, fraction = 0.5f)),
            start = Offset(x = center.x, y = center.y - size),
            end = Offset(x = center.x, y = center.y + size),
        ),
        alpha = alpha,
    )
    drawPath(
        path = shard,
        color = stoneLight,
        style = Stroke(width = 1.dp.toPx()),
        alpha = alpha * 0.6f,
    )
}

@Stable
private class StoneFistStrikeState {
    val slam = Animatable(initialValue = 0f)
    val shatter = Animatable(initialValue = 0f)
    val alpha = Animatable(initialValue = 0f)
    val impactAlpha = Animatable(initialValue = 0f)
    val impactScale = Animatable(initialValue = 0f)

    var scene by mutableStateOf<StoneFistGeometry?>(value = null)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GolemStoneFistKnockoutPreview() {
    GolemStoneFistPreview(
        normalRollMessageType = NormalRollMessageType.Knockout,
        accentColor = CharacterClassType.Necromancer.toColor(),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GolemStoneFistDamagePreview() {
    GolemStoneFistPreview(
        normalRollMessageType = NormalRollMessageType.Damage,
        accentColor = CharacterClassType.Illusionist.toColor(),
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun GolemStoneFistMissPreview() {
    GolemStoneFistPreview(
        normalRollMessageType = NormalRollMessageType.ArmorClassMiss,
        accentColor = CharacterClassType.Necromancer.toColor(),
    )
}

@Composable
private fun GolemStoneFistPreview(normalRollMessageType: NormalRollMessageType, accentColor: Color) {
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
                attackerClassName = "Necromancer",
                attackerClassType = CharacterClassType.Necromancer,
                impersonatedClassType = CharacterClassType.None,
                minionAttackerIndex = 0,
                minionAttackerType = MinionType.Golem,
                minionAttackerName = "Rocky",
                animation = ArenaAnimationState.Avatar(),
                gestures = emptyMap(),
                accentColor = accentColor,
                weaponType = WeaponType.StoneFist,
            )
        },
    ) { message ->
        GolemStoneFistOverlay(
            animatingMessage = message,
            attackerBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            defenderBounds = avatars.boundsFor(PREVIEW_TARGET_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
