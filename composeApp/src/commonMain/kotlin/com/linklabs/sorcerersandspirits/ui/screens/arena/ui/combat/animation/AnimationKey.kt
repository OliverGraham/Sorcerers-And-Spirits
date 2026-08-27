package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

internal sealed interface AnimationKey {
    data object FlashMessage : AnimationKey

    data object AvatarOverlay : AnimationKey

    data class AvatarGesture(val userId: String) : AnimationKey
}
