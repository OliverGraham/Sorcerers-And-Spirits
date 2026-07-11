package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.EncoreOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageSpellPotencyOverlay

/**
 * The Bard's Encore concert scene (SS-206), hosted over the whole screen rather than the seating
 * region so the darkened stage covers everything up to the edges. It sits below the combat buttons
 * in the arena's z-order, so while the stage holds waiting for the Encore roll the buttons float on
 * top of the darkness and stay usable. Draws nothing between casts.
 */
@Composable
internal fun EncoreFullScreenOverlay(
    animationCue: CombatAnimationCue?,
    presentClasses: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    if (CharacterClassType.Bard !in presentClasses) return

    EncoreOverlay(animationCue = animationCue, modifier = modifier.fillMaxSize())
}

/**
 * The Mage's Spell Potency storm (SS-210), hosted over the whole screen and above the combat
 * buttons in the arena's z-order, so a doubles-triggered area-of-effect engulfs the entire screen —
 * the buttons included — rather than stopping at the seating region. Anchored to the measured seats
 * through [CombatAvatarBounds.fullScreenBoundsFor], which resolves them in this full-screen host's
 * own coordinate space. Draws nothing between casts.
 */
@Composable
internal fun SpellPotencyFullScreenOverlay(
    animationCue: CombatAnimationCue?,
    avatarBounds: CombatAvatarBounds,
    presentClasses: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    if (CharacterClassType.Mage !in presentClasses) return

    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                avatarBounds.reportFullScreenOverlayBounds(it.boundsInRoot())
            }
    ) {
        MageSpellPotencyOverlay(
            animationCue = animationCue,
            avatarBoundsFor = avatarBounds::fullScreenBoundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
