package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

private const val HURT_TINT_MAX_RED_OFFSET = 130f
private const val HURT_TINT_CHANNEL_DAMP = 0.45f
private const val ARMOR_TINT_MAX_BLUE_OFFSET = 110f
private const val ARMOR_TINT_CHANNEL_DAMP = 0.3f

// TODO: Should be named "AvatarGestureStateHolder"
@Stable
internal class AvatarGestureState {
    val offsetX = Animatable(initialValue = 0f)
    val offsetY = Animatable(initialValue = 0f)
    val rotation = Animatable(initialValue = 0f)
    val scaleX = Animatable(initialValue = 1f)
    val scaleY = Animatable(initialValue = 1f)
    val collapseFraction = Animatable(initialValue = 0f)
    val hurtTint = Animatable(initialValue = 0f)
    val armorTint = Animatable(initialValue = 0f)
    val saturation = Animatable(initialValue = 1f)

    val mirrorSplit = Animatable(initialValue = 0f)
    val mirrorReturn = Animatable(initialValue = 0f)
    val mirrorFakeVanish = Animatable(initialValue = 0f)

    val teleportVanish = Animatable(initialValue = 0f)

    val colorFilter: ColorFilter?
        get() {
            val isAtRest = hurtTint.value == 0f && armorTint.value == 0f && saturation.value == 1f
            if (isAtRest) return null

            val matrix = ColorMatrix().apply { setToSaturation(sat = saturation.value) }
            matrix *= hurtTintMatrix(tint = hurtTint.value)
            matrix *= armorTintMatrix(tint = armorTint.value)
            return ColorFilter.colorMatrix(colorMatrix = matrix)
        }
}

private fun armorTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - ARMOR_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                ARMOR_TINT_MAX_BLUE_OFFSET * tint,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}

private fun hurtTintMatrix(tint: Float): ColorMatrix {
    val damp = 1f - HURT_TINT_CHANNEL_DAMP * tint
    return ColorMatrix(
        values =
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                HURT_TINT_MAX_RED_OFFSET * tint,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                damp,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
            )
    )
}
