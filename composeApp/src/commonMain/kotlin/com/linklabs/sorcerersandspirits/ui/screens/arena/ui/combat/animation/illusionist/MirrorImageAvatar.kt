package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.REACTION_DELAY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarImage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.feetPivot
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.collapse
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flinch
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flinchHeavy
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.sidestep
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.snapToRest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MIRROR_SPLIT_OFFSET_FRACTION = 0.28f
private const val MIRROR_COPY_ALPHA = 0.72f
private const val MIRROR_POOF_SCALE_GROWTH = 0.2f
private const val MIRROR_POOF_RISE_FRACTION = 0.12f

private const val MIRROR_SPLIT_MILLIS = 360
private const val MIRROR_SPLIT_HOLD_MILLIS = 160L
private const val MIRROR_POOF_MILLIS = 340
private const val MIRROR_RETURN_MILLIS = 320

@Composable
internal fun MirrorSplitClassAvatar(
    characterClass: CharacterClassState,
    gestureState: AvatarGestureState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AvatarImage(
            characterClass = characterClass,
            modifier =
                Modifier.matchParentSize().graphicsLayer {
                    applyMirrorFake(gestureState = gestureState)
                },
        )
        AvatarImage(
            characterClass = characterClass,
            colorFilter = gestureState.colorFilter,
            modifier =
                Modifier.matchParentSize().graphicsLayer {
                    applyMirrorReal(gestureState = gestureState)
                },
        )
    }
}

private fun GraphicsLayerScope.applyMirrorFake(gestureState: AvatarGestureState) {
    val split = gestureState.mirrorSplit.value
    val vanish = gestureState.mirrorFakeVanish.value
    transformOrigin = feetPivot
    translationX = -size.width * MIRROR_SPLIT_OFFSET_FRACTION * split
    translationY = -size.height * MIRROR_POOF_RISE_FRACTION * vanish
    val grow = 1f + MIRROR_POOF_SCALE_GROWTH * vanish
    scaleX = grow
    scaleY = grow
    alpha = MIRROR_COPY_ALPHA * (1f - vanish)
}

private fun GraphicsLayerScope.applyMirrorReal(gestureState: AvatarGestureState) {
    applyGesture(gestureState = gestureState)
    val split = gestureState.mirrorSplit.value
    val returnFraction = gestureState.mirrorReturn.value
    translationX += size.width * MIRROR_SPLIT_OFFSET_FRACTION * split * (1f - returnFraction)
    alpha = MIRROR_COPY_ALPHA + (1f - MIRROR_COPY_ALPHA) * returnFraction
}

internal fun ArenaMessageState.getMirrorImageMessageOrNull(
    userId: String
): ArenaMessageState.MirrorImage? =
    (this as? ArenaMessageState.MirrorImage)?.takeIf { it.illusionistUserId == userId }

internal suspend fun AvatarGestureState.playMirrorImage(
    message: ArenaMessageState.MirrorImage,
    density: Density,
) {
    when (message) {
        is ArenaMessageState.MirrorImage.Pass ->
            mirrorResolve(messageType = message.messageType, density = density)

        is ArenaMessageState.MirrorImage.TemptFate ->
            mirrorResolve(messageType = message.messageType, density = density)

        is ArenaMessageState.MirrorImage.Trigger -> mirrorSplitOut()
    }
}

private suspend fun AvatarGestureState.mirrorSplitOut() {
    snapToRest()
    mirrorSplit.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MIRROR_SPLIT_MILLIS, easing = FastOutSlowInEasing),
    )
}

private suspend fun AvatarGestureState.mirrorResolve(
    messageType: NormalRollMessageType?,
    density: Density,
) {
    if (mirrorSplit.value == 0f) mirrorSplit.snapTo(targetValue = 1f)
    mirrorReturn.snapTo(targetValue = 0f)
    mirrorFakeVanish.snapTo(targetValue = 0f)

    coroutineScope {
        launch {
            mirrorFakeVanish.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = MIRROR_POOF_MILLIS),
            )
        }
        launch {
            mirrorReturn.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = MIRROR_RETURN_MILLIS, easing = FastOutSlowInEasing),
            )
        }
        if (messageType != null) {
            launch { playDefenderReaction(messageType = messageType, density = density) }
        }
    }

    mirrorSplit.snapTo(targetValue = 0f)
    mirrorReturn.snapTo(targetValue = 0f)
    mirrorFakeVanish.snapTo(targetValue = 0f)
}

private suspend fun AvatarGestureState.playDefenderReaction(
    messageType: NormalRollMessageType,
    density: Density,
) =
    with(density) {
        when (messageType) {
            NormalRollMessageType.ArmorClassHit ->
                flinch(
                    shakePx = 10.dp.toPx(),
                    tintPeak = 0.7f,
                    reactionDelayMillis = REACTION_DELAY_MILLIS,
                )

            NormalRollMessageType.ArmorClassMiss ->
                sidestep(distancePx = 40.dp.toPx(), reactionDelayMillis = REACTION_DELAY_MILLIS)

            NormalRollMessageType.Damage ->
                flinchHeavy(shakePx = 16.dp.toPx(), reactionDelayMillis = REACTION_DELAY_MILLIS)

            NormalRollMessageType.Knockout -> collapse(reactionDelayMillis = REACTION_DELAY_MILLIS)
        }
    }
