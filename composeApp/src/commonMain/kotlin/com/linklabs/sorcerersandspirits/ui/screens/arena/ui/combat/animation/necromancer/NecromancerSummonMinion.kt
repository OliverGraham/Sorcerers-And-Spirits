package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.summonminion.MinionType
import com.linklabs.sorcerersandspirits.ui.screens.arena.AnimationCompleteArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaAnimationState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.SummonMinionOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationKey
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.AnimationPreview
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_ACTOR_ID
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_HEIGHT_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.PREVIEW_WIDTH_DP
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rememberSoloAvatar
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.ic_health
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val PORTAL_OPEN_MILLIS = 340
private const val PORTAL_HOLD_MILLIS = 160L
private const val PORTAL_CLOSE_MILLIS = 300
private const val FIZZLE_GRAY_MILLIS = 260
private const val FIZZLE_HOLD_MILLIS = 140L
private const val MINION_TRAVEL_MILLIS = 620
private const val HEAL_MILLIS = 900

private const val MINION_ENTRANCE_MIN_SCALE = 0.25f
private const val MINION_ENTRANCE_ARC_FRACTION = 0.12f
private const val HEAL_BURST_FRACTION = 0.4f
private const val HEAL_MOTE_COUNT = 5

private val healRed = Color(color = 0xFFE5484D)
private val healGreen = necroGlowGreen

@Composable
internal fun NecromancerSummonMinionOverlay(
    animatingMessage: ArenaMessageState.SummonMinion.Summon,
    avatarBounds: Rect,
    modifier: Modifier = Modifier,
) {
    val state = remember { SummonMinionEffectState() }
    val density = LocalDensity.current
    val onAnimationComplete = ArenaActionsProvider.current.onAnimationComplete

    LaunchedEffect(key1 = animatingMessage.id) {
        state.play(
            scene =
            summonMinionScene(
                bounds = avatarBounds,
                outcome = animatingMessage.outcome,
                minionIndex = animatingMessage.minionIndex,
                density = density,
                seed = animatingMessage.id.hashCode(),
                portalPalette =
                summonPortalPaletteFor(actorClass = animatingMessage.actingClassType),
            ),
            outcome = animatingMessage.outcome,
        )

        onAnimationComplete(
            AnimationCompleteArguments(
                messageId = animatingMessage.id,
                animationKey = AnimationKey.AvatarOverlay,
            ),
        )
    }

    val healIcon = imageResource(resource = Res.drawable.ic_health)
    val skeletonImage = imageResource(resource = Res.drawable.skeleton_minion)
    val golemImage = imageResource(resource = Res.drawable.golem_minion)
    Canvas(modifier = modifier) {
        drawSummonMinion(
            state = state,
            healIcon = healIcon,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private suspend fun SummonMinionEffectState.play(
    scene: SummonMinionScene,
    outcome: SummonMinionOutcome,
) {
    portal.snapTo(targetValue = 0f)
    gray.snapTo(targetValue = 0f)
    creature.snapTo(targetValue = 0f)
    heal.snapTo(targetValue = 0f)
    this.scene = scene

    portal.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = PORTAL_OPEN_MILLIS, easing = FastOutSlowInEasing),
    )
    delay(timeMillis = PORTAL_HOLD_MILLIS)

    when (outcome) {
        SummonMinionOutcome.Fizzle -> {
            fizzle()
            this.scene = null
        }

        SummonMinionOutcome.Skeleton -> resolveSummon(heals = false)

        SummonMinionOutcome.SkeletonAndHeal,
        SummonMinionOutcome.Golem,
        -> resolveSummon(heals = true)
    }
}

private suspend fun SummonMinionEffectState.fizzle() {
    gray.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = FIZZLE_GRAY_MILLIS))
    delay(timeMillis = FIZZLE_HOLD_MILLIS)
    portal.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = PORTAL_CLOSE_MILLIS))
}

private suspend fun SummonMinionEffectState.resolveSummon(heals: Boolean) {
    coroutineScope {
        launch {
            creature.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = MINION_TRAVEL_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        if (heals) {
            launch {
                heal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = HEAL_MILLIS),
                )
            }
        }
        delay(timeMillis = if (heals) HEAL_MILLIS.toLong() else MINION_TRAVEL_MILLIS.toLong())
    }

    portal.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = PORTAL_CLOSE_MILLIS),
    )
}

private fun summonPortalPaletteFor(actorClass: CharacterClassType): ScythePalette = if (actorClass == CharacterClassType.Illusionist) {
    illusionistScythePalette
} else {
    necroScythePalette
}

private fun summonMinionScene(
    bounds: Rect,
    outcome: SummonMinionOutcome,
    minionIndex: Int,
    density: Density,
    seed: Int,
    portalPalette: ScythePalette,
): SummonMinionScene {
    val center = bounds.center
    val portalCenter = Offset(x = center.x, y = bounds.bottom - bounds.height * 0.05f)
    val heals =
        outcome == SummonMinionOutcome.SkeletonAndHeal || outcome == SummonMinionOutcome.Golem

    val minion = minionFor(outcome = outcome)
    val restingSlot =
        if (minion != null) {
            minionRestingSlot(
                displayIndex = minionIndex.coerceAtMost(maximumValue = MAX_RENDERED_MINIONS - 1),
                minionType = minion,
                area = bounds,
            )
        } else {
            null
        }

    val random = Random(seed = seed)
    val healMotes =
        if (heals) {
            (0 until HEAL_MOTE_COUNT).map { index ->
                HealMote(
                    x = center.x + random.nextFloat(min = -0.5f, max = 0.5f) * bounds.width,
                    baseY = center.y + bounds.height * 0.1f,
                    risePx = bounds.height * random.nextFloat(min = 0.4f, max = 0.7f),
                    bobPx = with(density) { random.nextFloat(min = 5f, max = 11f).dp.toPx() },
                    sizePx = with(density) { random.nextFloat(min = 16f, max = 26f).dp.toPx() },
                    phase = index.toFloat() / HEAL_MOTE_COUNT,
                )
            }
        } else {
            emptyList()
        }

    return SummonMinionScene(
        portalCenter = portalCenter,
        portalPalette = portalPalette,
        portalRadiusPx = bounds.width * 0.42f,
        minion = minion,
        restingSlot = restingSlot,
        heals = heals,
        healCenter = center,
        healRadiusPx = bounds.width * 0.7f,
        healMotes = healMotes,
    )
}

private fun DrawScope.drawSummonMinion(
    state: SummonMinionEffectState,
    healIcon: ImageBitmap,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val scene = state.scene ?: return
    val open = state.portal.value

    drawSummonPortal(
        center = scene.portalCenter,
        radius = scene.portalRadiusPx,
        open = open,
        gray = state.gray.value,
        alpha = 1f,
        palette = scene.portalPalette,
    )

    val minion = scene.minion
    val restingSlot = scene.restingSlot
    val emerge = state.creature.value
    if (minion != null && restingSlot != null && emerge > 0f) {
        drawMinion(
            kind = minion,
            footCenter =
            minionEntranceFootCenter(
                progress = emerge,
                portalCenter = scene.portalCenter,
                restingCenter = restingSlot.footCenter,
                arcHeight = size.height * MINION_ENTRANCE_ARC_FRACTION,
            ),
            height = restingSlot.height * (MINION_ENTRANCE_MIN_SCALE + (1f - MINION_ENTRANCE_MIN_SCALE) * emerge),
            width = restingSlot.height * (MINION_ENTRANCE_MIN_SCALE + (1f - MINION_ENTRANCE_MIN_SCALE) * emerge),
            alpha = 1f,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }

    val heal = state.heal.value
    if (scene.heals && heal > 0f) {
        drawHealBurst(center = scene.healCenter, radius = scene.healRadiusPx, heal = heal)
        scene.healMotes.forEach { mote -> drawHealMote(mote = mote, heal = heal, icon = healIcon) }
    }
}

private fun minionEntranceFootCenter(
    progress: Float,
    portalCenter: Offset,
    restingCenter: Offset,
    arcHeight: Float,
): Offset {
    val x = portalCenter.x + (restingCenter.x - portalCenter.x) * progress
    val arc = sin(progress * PI.toFloat()) * arcHeight
    val y = portalCenter.y + (restingCenter.y - portalCenter.y) * progress - arc
    return Offset(x = x, y = y)
}

private fun DrawScope.drawHealBurst(center: Offset, radius: Float, heal: Float) {
    val burst = (heal / HEAL_BURST_FRACTION).coerceAtMost(maximumValue = 1f)
    val fade = 1f - burst
    if (fade <= 0f) return

    val outerRadius = radius * (0.3f + 0.7f * burst)
    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(healGreen.copy(alpha = 0.8f), Color.Transparent),
            center = center,
            radius = outerRadius,
        ),
        radius = outerRadius,
        center = center,
        alpha = fade,
    )
    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(healRed.copy(alpha = 0.7f), Color.Transparent),
            center = center,
            radius = outerRadius * 0.6f,
        ),
        radius = outerRadius * 0.6f,
        center = center,
        alpha = fade,
    )
}

private fun DrawScope.drawHealMote(mote: HealMote, heal: Float, icon: ImageBitmap) {
    val bob = sin((heal * 3f + mote.phase) * 2f * PI.toFloat()) * mote.bobPx
    val fadeIn = (heal / 0.2f).coerceAtMost(maximumValue = 1f)
    val fadeOut = 1f - ((heal - 0.7f) / 0.3f).coerceIn(minimumValue = 0f, maximumValue = 1f)
    val alpha = fadeIn * fadeOut
    if (alpha <= 0.01f) return

    val x = mote.x
    val y = mote.baseY - mote.risePx * heal + bob
    val size = mote.sizePx.roundToInt()
    drawImage(
        image = icon,
        dstOffset =
        IntOffset(
            x = (x - mote.sizePx / 2f).roundToInt(),
            y = (y - mote.sizePx / 2f).roundToInt(),
        ),
        dstSize = IntSize(width = size, height = size),
        alpha = alpha,
    )
}

private fun Random.nextFloat(min: Float, max: Float): Float = nextFloat() * (max - min) + min

@Stable
private class SummonMinionEffectState {
    val portal = Animatable(initialValue = 0f)
    val gray = Animatable(initialValue = 0f)
    val creature = Animatable(initialValue = 0f)
    val heal = Animatable(initialValue = 0f)

    var scene by mutableStateOf<SummonMinionScene?>(value = null)
}

@Immutable
private data class SummonMinionScene(
    val portalCenter: Offset,
    val portalPalette: ScythePalette,
    val portalRadiusPx: Float,
    val minion: MinionType?,
    val restingSlot: MinionSlot?,
    val heals: Boolean,
    val healCenter: Offset,
    val healRadiusPx: Float,
    val healMotes: List<HealMote>,
)

@Immutable
private data class HealMote(
    val x: Float,
    val baseY: Float,
    val risePx: Float,
    val bobPx: Float,
    val sizePx: Float,
    val phase: Float,
)

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerSummonMinionFizzlePreview() {
    NecromancerSummonMinionPreview(outcome = SummonMinionOutcome.Fizzle)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerSummonMinionSkeletonPreview() {
    NecromancerSummonMinionPreview(outcome = SummonMinionOutcome.Skeleton)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerSummonMinionSkeletonAndHealPreview() {
    NecromancerSummonMinionPreview(outcome = SummonMinionOutcome.SkeletonAndHeal)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun NecromancerSummonMinionGolemPreview() {
    NecromancerSummonMinionPreview(outcome = SummonMinionOutcome.Golem)
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistSummonMinionSkeletonPreview() {
    NecromancerSummonMinionPreview(
        outcome = SummonMinionOutcome.Skeleton,
        actorClass = CharacterClassType.Illusionist,
    )
}

@Preview(widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun IllusionistSummonMinionGolemPreview() {
    NecromancerSummonMinionPreview(
        outcome = SummonMinionOutcome.Golem,
        actorClass = CharacterClassType.Illusionist,
    )
}

@Composable
private fun NecromancerSummonMinionPreview(
    outcome: SummonMinionOutcome,
    actorClass: CharacterClassType = CharacterClassType.Necromancer,
) {
    val avatars = rememberSoloAvatar()
    AnimationPreview(
        accentClass = actorClass,
        avatars = avatars,
        buildMessage = { id ->
            ArenaMessageState.SummonMinion.Summon(
                id = id,
                necromancerUserId = PREVIEW_ACTOR_ID,
                className = actorClass.name,
                actingClassType = CharacterClassType.Necromancer,
                singleDie =
                when (outcome) {
                    SummonMinionOutcome.Fizzle -> 1
                    SummonMinionOutcome.Skeleton -> 2
                    SummonMinionOutcome.SkeletonAndHeal -> 4
                    SummonMinionOutcome.Golem -> 6
                },
                outcome = outcome,
                minionIndex = 0,
                minionName = "Kebab",
                animation = ArenaAnimationState.FlashAndAvatar(),
                gestures = emptyMap(),
            )
        },
    ) { message ->
        NecromancerSummonMinionOverlay(
            animatingMessage = message,
            avatarBounds = avatars.boundsFor(PREVIEW_ACTOR_ID),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// TODO(SS-268): move this outcome->MinionType mapping out of the animation file into the
// ArenaMessageState.SummonMinion toState mapping (carry minionType on the message state); flagged by SS-269.
private fun minionFor(outcome: SummonMinionOutcome): MinionType? = when (outcome) {
    SummonMinionOutcome.Fizzle -> null

    SummonMinionOutcome.Skeleton,
    SummonMinionOutcome.SkeletonAndHeal,
    -> MinionType.Skeleton

    SummonMinionOutcome.Golem -> MinionType.Golem
}
