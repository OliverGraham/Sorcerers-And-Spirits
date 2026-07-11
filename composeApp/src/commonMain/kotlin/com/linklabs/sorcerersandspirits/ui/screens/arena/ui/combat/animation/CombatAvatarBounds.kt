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
    private var fullScreenOverlayBounds by mutableStateOf(value = Rect.Zero)

    fun reportAvatarBounds(userId: String, boundsInRoot: Rect) {
        boundsByUserId[userId] = boundsInRoot
    }

    fun reportOverlayBounds(boundsInRoot: Rect) {
        overlayBounds = boundsInRoot
    }

    /** The root-space bounds of a full-screen effect host that spans the whole screen. */
    fun reportFullScreenOverlayBounds(boundsInRoot: Rect) {
        fullScreenOverlayBounds = boundsInRoot
    }

    fun boundsFor(userId: String): Rect? {
        val boundsInRoot = boundsByUserId[userId] ?: return null
        return boundsInRoot.translate(offset = -overlayBounds.topLeft)
    }

    /**
     * A seat's bounds in the coordinate space of a full-screen effect host (SS-210/SS-206), which
     * spans the whole screen rather than the seating region, so effects hosted over the buttons
     * still land on the right avatar.
     */
    fun fullScreenBoundsFor(userId: String): Rect? {
        val boundsInRoot = boundsByUserId[userId] ?: return null
        return boundsInRoot.translate(offset = -fullScreenOverlayBounds.topLeft)
    }
}

/**
 * The character bust art's width-to-height ratio. The avatar image is drawn ContentScale.Fit and
 * centered inside a seat box that fills the full stage width, so the reported bounds are far wider
 * than the visible character. Effects sized off that raw width balloon across the screen; sizing
 * off [visibleAvatarWidth] keeps them anchored to the character the player actually sees.
 */
private const val AVATAR_ART_ASPECT = 0.75f

/** The on-screen width of the visible character within its letterboxed seat bounds. */
internal fun Rect.visibleAvatarWidth(): Float = height * AVATAR_ART_ASPECT
