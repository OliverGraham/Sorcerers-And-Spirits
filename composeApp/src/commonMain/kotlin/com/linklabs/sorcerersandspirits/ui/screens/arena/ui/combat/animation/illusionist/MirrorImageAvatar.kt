package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarGestureState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.AvatarImage
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.applyGesture
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common.feetPivot

private const val MIRROR_SPLIT_OFFSET_FRACTION = 0.28f
private const val MIRROR_COPY_ALPHA = 0.72f
private const val MIRROR_POOF_SCALE_GROWTH = 0.2f
private const val MIRROR_POOF_RISE_FRACTION = 0.12f

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
