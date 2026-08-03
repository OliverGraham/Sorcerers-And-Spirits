package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.EncoreOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageSpellPotencyOverlay

@Composable
internal fun EncoreFullScreenOverlay(
    animatingMessage: ArenaMessageState?,
    presentClasses: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    if (CharacterClassType.Bard !in presentClasses) return

    EncoreOverlay(animatingMessage = animatingMessage, modifier = modifier.fillMaxSize())
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
    animatingMessage: ArenaMessageState?,
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
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::fullScreenBoundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
