package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin

private const val COLLAPSE_ROTATION_DEGREES = 80f
private const val COLLAPSE_FEET_SLIDE_FRACTION = 0.4f

internal val feetPivot = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)

internal fun GraphicsLayerScope.applyGesture(gestureState: AvatarGestureState) {
    val collapse = gestureState.collapseFraction.value
    val lyingScale = (size.width / size.height).coerceAtMost(maximumValue = 1f)
    val collapseScale = 1f + (lyingScale - 1f) * collapse

    transformOrigin = feetPivot
    translationX = gestureState.offsetX.value - size.width * COLLAPSE_FEET_SLIDE_FRACTION * collapse
    translationY = gestureState.offsetY.value
    rotationZ = gestureState.rotation.value + COLLAPSE_ROTATION_DEGREES * collapse
    scaleX = gestureState.scaleX.value * collapseScale
    scaleY = gestureState.scaleY.value * collapseScale
    alpha = 1f - gestureState.teleportVanish.value
}
