package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
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
    animationCue: CombatAnimationCue?,
    avatarBounds: CombatAvatarBounds,
    presentClasses: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                avatarBounds.reportOverlayBounds(boundsInRoot = it.boundsInRoot())
            }
    ) {
        if (CharacterClassType.Rogue in presentClasses) {
            RogueNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Gambler in presentClasses) {
            GamblerNormalAttackOverlay(
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
        }

        if (CharacterClassType.Necromancer in presentClasses) {
            NecromancerNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            NecromancerSummonMinionOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Bard in presentClasses) {
            BardNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PersuadeOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Duelist in presentClasses) {
            DuelistNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PrepareAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            RiposteOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Mage in presentClasses) {
            MageNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            MageTeleportOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Paladin in presentClasses) {
            PaladinNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            PaladinHolyShieldOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (CharacterClassType.Illusionist in presentClasses) {
            IllusionistNormalAttackOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
            ImpersonateOverlay(
                animationCue = animationCue,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
