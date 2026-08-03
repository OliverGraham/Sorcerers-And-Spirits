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
    characterTypesInArena: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    if (CharacterClassType.Bard !in characterTypesInArena) return

    EncoreOverlay(animatingMessage = animatingMessage, modifier = modifier.fillMaxSize())
}

@Composable
internal fun SpellPotencyFullScreenOverlay(
    animatingMessage: ArenaMessageState?,
    avatarBounds: CombatAvatarBounds,
    characterTypesInArena: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    if (CharacterClassType.Mage !in characterTypesInArena) return

    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                avatarBounds.reportFullScreenOverlayBounds(boundsInRoot = it.boundsInRoot())
            }
    ) {
        MageSpellPotencyOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::fullScreenBoundsFor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
