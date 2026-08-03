package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BardNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.PersuadeOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.DuelistNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PrepareAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RiposteOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.AllInOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.GamblerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.LuckyCoinOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.IllusionistNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.ImpersonateOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageTeleportOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerSummonMinionOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.PaladinHolyShieldOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.PaladinNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.RogueNormalAttackOverlay

@Composable
internal fun CombatAnimationOverlays(
    animatingMessage: ArenaMessageState?,
    avatarBounds: CombatAvatarBounds,
    characterTypesInArena: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                avatarBounds.reportOverlayBounds(boundsInRoot = it.boundsInRoot())
            }
    ) {
        if (CharacterClassType.Rogue in characterTypesInArena) {
            RogueNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Gambler in characterTypesInArena) {
            GamblerNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            AllInOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            LuckyCoinOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Necromancer in characterTypesInArena) {
            NecromancerNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            NecromancerSummonMinionOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Bard in characterTypesInArena) {
            BardNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PersuadeOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Duelist in characterTypesInArena) {
            DuelistNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PrepareAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            RiposteOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Mage in characterTypesInArena) {
            MageNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            MageTeleportOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Paladin in characterTypesInArena) {
            PaladinNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PaladinHolyShieldOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Illusionist in characterTypesInArena) {
            IllusionistNormalAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            ImpersonateOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
