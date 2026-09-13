package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val EXPLODE_MILLIS = 620
private const val SHARDS_PER_MINION = 12
private const val BODY_FADE_FRACTION = 0.45f
private const val BURST_RAMP_FRACTION = 0.25f

@Composable
internal fun MinionDeathOverlay(
    messageId: String,
    minionType: MinionType,
    minionIndex: Int,
    avatarBounds: Rect,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(initialValue = 0f) }
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = messageId) {
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = EXPLODE_MILLIS, easing = LinearOutSlowInEasing),
        )

        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = messageId,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    val dyingMinion =
        remember(key1 = messageId, key2 = avatarBounds) {
            buildDyingMinion(
                messageId = messageId,
                minionType = minionType,
                minionIndex = minionIndex,
                area = avatarBounds,
            )
        }
    val skeletonImage = imageResource(resource = Res.drawable.skeleton_minion)
    val golemImage = imageResource(resource = Res.drawable.golem_minion)

    Canvas(modifier = modifier) {
        drawDyingMinion(
            dyingMinion = dyingMinion,
            progress = progress.value,
            accentColor = accentColor,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private fun buildDyingMinion(
    messageId: String,
    minionType: MinionType,
    minionIndex: Int,
    area: Rect,
): DyingMinion {
    val slot = minionRestingSlot(displayIndex = minionIndex, minionType = minionType, area = area)
    val random = Random(seed = messageId.hashCode() + minionIndex)
    val burstCenter = Offset(x = slot.footCenter.x, y = slot.footCenter.y - slot.height * 0.5f)

    val shards =
        (0 until SHARDS_PER_MINION).map {
            Shard(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                distance = slot.height * random.nextFloat(min = 0.3f, max = 0.95f),
                size = slot.height * random.nextFloat(min = 0.06f, max = 0.15f),
                lightness = random.nextFloat(),
                spinTurns = random.nextFloat(min = -1.2f, max = 1.2f),
            )
        }

    return DyingMinion(
        type = minionType,
        footCenter = slot.footCenter,
        height = slot.height,
        burstCenter = burstCenter,
        shards = shards,
    )
}

private fun DrawScope.drawDyingMinion(
    dyingMinion: DyingMinion,
    progress: Float,
    accentColor: Color,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val bodyFade = 1f - (progress / BODY_FADE_FRACTION).coerceAtMost(maximumValue = 1f)
    if (bodyFade > 0f) {
        val lift = dyingMinion.height * 0.12f * progress
        drawMinion(
            kind = dyingMinion.type,
            footCenter =
            Offset(x = dyingMinion.footCenter.x, y = dyingMinion.footCenter.y - lift),
            height = dyingMinion.height,
            width = dyingMinion.height,
            alpha = bodyFade,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }

    drawBurst(
        center = dyingMinion.burstCenter,
        maxRadius = dyingMinion.height * 0.85f,
        progress = progress,
        accentColor = accentColor,
    )

    dyingMinion.shards.forEach { shard ->
        drawShard(
            shard = shard,
            origin = dyingMinion.burstCenter,
            progress = progress,
            accentColor = accentColor,
        )
    }
}

private fun DrawScope.drawBurst(
    center: Offset,
    maxRadius: Float,
    progress: Float,
    accentColor: Color,
) {
    val radius = maxRadius * progress
    if (radius <= 0f) return

    val rampIn = (progress / BURST_RAMP_FRACTION).coerceAtMost(maximumValue = 1f)
    val alpha = rampIn * (1f - progress)
    if (alpha <= 0.01f) return

    drawCircle(
        brush =
        Brush.radialGradient(
            colors =
            listOf(
                Color.White.copy(alpha = 0.9f),
                accentColor.copy(alpha = 0.7f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )
}

private fun DrawScope.drawShard(
    shard: Shard,
    origin: Offset,
    progress: Float,
    accentColor: Color,
) {
    val alpha = 1f - progress
    if (alpha <= 0.01f) return

    val travel = shard.distance * progress
    val fall = shard.distance * 0.4f * progress * progress
    val position =
        Offset(
            x = origin.x + cos(shard.angle) * travel,
            y = origin.y + sin(shard.angle) * travel + fall,
        )
    val color = lerp(start = accentColor, stop = Color.White, fraction = shard.lightness * 0.5f)
    val size = shard.size * (1f - 0.4f * progress)

    rotate(degrees = shard.spinTurns * 360f * progress, pivot = position) {
        drawRect(
            color = color,
            topLeft = Offset(x = position.x - size / 2f, y = position.y - size / 2f),
            size = Size(width = size, height = size),
            alpha = alpha,
        )
    }
}

private fun Random.nextFloat(min: Float, max: Float): Float = nextFloat() * (max - min) + min

@Immutable
private data class DyingMinion(
    val type: MinionType,
    val footCenter: Offset,
    val height: Float,
    val burstCenter: Offset,
    val shards: List<Shard>,
)

@Immutable
private data class Shard(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val lightness: Float,
    val spinTurns: Float,
)

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MinionDeathSkeletonPreview() {
    MinionDeathPreview(minionType = MinionType.Skeleton)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun MinionDeathGolemPreview() {
    MinionDeathPreview(minionType = MinionType.Golem)
}

@Composable
private fun MinionDeathPreview(minionType: MinionType) {
    val accentClass = CharacterClassType.Necromancer
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = accentClass,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.SummonMinion.MinionDestroyed(
                id = id,
                actingClassType = accentClass,
                summonerUserId = PREVIEW_ACTOR_ID,
                summonerClassName = accentClass.name,
                minionName = accentClass.name,
                minionType = minionType,
                minionIndex = 0,
                damageAbsorbed = 1,
            )
        },
    ) { message ->
        MinionDeathOverlay(
            messageId = message.id,
            minionType = message.minionType,
            minionIndex = message.minionIndex,
            avatarBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            accentColor = accentClass.toColor(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
