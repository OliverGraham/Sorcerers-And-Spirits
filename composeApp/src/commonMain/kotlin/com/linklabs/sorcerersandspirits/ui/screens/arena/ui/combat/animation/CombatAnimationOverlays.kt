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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.DuelistNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PrepareAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RiposteOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.AllInOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.GamblerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.LuckyCoinOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerSummonMinionOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.RogueNormalAttackOverlay

/**
 * The weapon and ability effect layers of the combat stage, drawn across both seats. Hosted above
 * the seating transition rather than inside it: a knockout re-seats the stage in the same frame its
 * animation cue goes live, and an overlay living inside the swapped pane would be recreated mid-cue
 * and skip the whole effect (SS-203). Up here the overlays outlive any seat change and play every
 * cue exactly once.
 *
 * Only the overlays for classes actually in the arena are hosted: a class's effect can never fire
 * unless that class is seated, so an arena with no Gambler never composes the All-In or Lucky Coin
 * layers. New per-class effects gate themselves on [presentClasses] the same way.
 */
@Composable
internal fun CombatAnimationOverlays(
    animationCue: CombatAnimationCue?,
    avatarBounds: CombatAvatarBounds,
    presentClasses: Set<CharacterClassType>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.onGloballyPositioned { avatarBounds.reportOverlayBounds(it.boundsInRoot()) }
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
    }
}
