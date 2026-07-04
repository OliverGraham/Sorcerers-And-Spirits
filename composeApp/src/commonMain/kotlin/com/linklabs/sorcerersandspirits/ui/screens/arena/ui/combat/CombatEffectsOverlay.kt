package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue

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

/**
 * The weapon and ability effect layers of the combat stage, drawn across both seats. Hosted above
 * the seating transition rather than inside it: a knockout re-seats the stage in the same frame its
 * animation cue goes live, and an overlay living inside the swapped pane would be recreated mid-cue
 * and skip the whole effect (SS-203). Up here the overlays outlive any seat change and play every
 * cue exactly once.
 */
@Composable
internal fun CombatEffectsOverlay(
    animationCue: CombatAnimationCue?,
    avatarBounds: CombatAvatarBounds,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.onGloballyPositioned { avatarBounds.reportOverlayBounds(it.boundsInRoot()) }
    ) {
        WeaponStrikeOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = Modifier.fillMaxSize(),
        )

        AllInOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = Modifier.fillMaxSize(),
        )

        LuckyCoinOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = Modifier.fillMaxSize(),
        )

        PrepareAttackOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
