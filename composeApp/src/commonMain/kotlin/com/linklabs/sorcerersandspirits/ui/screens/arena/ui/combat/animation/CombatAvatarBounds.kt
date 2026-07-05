package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * The measured seat geometry the combat effect overlays aim at. The seated avatars report their
 * root-space bounds here as they lay out, and the overlay canvas reports its own, so [boundsFor]
 * answers in overlay-local coordinates. It lives above the seating transition and keeps the last
 * bounds reported per player, so an effect can still land on a seat whose pane is mid-swap.
 */
@Stable
internal class CombatAvatarBounds {
    private val boundsByUserId = mutableStateMapOf<String, Rect>()
    private var overlayBounds by mutableStateOf(value = Rect.Zero)

    fun reportAvatarBounds(userId: String, boundsInRoot: Rect) {
        boundsByUserId[userId] = boundsInRoot
    }

    fun reportOverlayBounds(boundsInRoot: Rect) {
        overlayBounds = boundsInRoot
    }

    fun boundsFor(userId: String): Rect? {
        val boundsInRoot = boundsByUserId[userId] ?: return null
        return boundsInRoot.translate(offset = -overlayBounds.topLeft)
    }
}
