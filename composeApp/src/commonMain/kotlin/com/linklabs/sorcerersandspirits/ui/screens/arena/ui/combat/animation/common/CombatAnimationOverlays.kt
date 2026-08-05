package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

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
    animatingMessage: ArenaMessageState,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                avatarBoundsStateHolder.reportOverlayBounds(boundsInRoot = it.boundsInRoot())
            }
    ) {
        CombatAnimationOverlaysWithBounds(
            animatingMessage = animatingMessage,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CombatAnimationOverlaysWithBounds(
    animatingMessage: ArenaMessageState,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    when (animatingMessage) {
        is ArenaMessageState.NormalRoll -> AnimateNormalAttack(
            animatingMessage = animatingMessage,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
        )
        is ArenaMessageState.AllIn -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.gamblerUserId) ?: return

            AllInOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                modifier = modifier,
            )
        }
        is ArenaMessageState.Dodge -> {
            // TODO: no sure where this one is
        }
        is ArenaMessageState.Encore -> {
            // TODO: is somewhere else, because it's full screen? Why aren't all just full screen
        }
        is ArenaMessageState.HolyShield -> PaladinHolyShieldOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        is ArenaMessageState.Impersonate -> ImpersonateOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        is ArenaMessageState.KillingMomentum -> {}
        is ArenaMessageState.LuckyCoin -> LuckyCoinOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        is ArenaMessageState.MirrorImage -> {
            // TODO: Where is it?
        }


        is ArenaMessageState.Persuade ->             PersuadeOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        is ArenaMessageState.PrepareAttack ->
            PrepareAttackOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = modifier,
            )
        is ArenaMessageState.Riposte ->
            RiposteOverlay(
                riposteMessage = animatingMessage,
                avatarBoundsFor = avatarBounds::boundsFor,
                modifier = modifier,
            )
        is ArenaMessageState.SpellPotency -> {
            // TODO: Wherever this is - move all to there? Any reason to not have everything have
            //       Have the capability for
        }
        is ArenaMessageState.SummonMinion -> NecromancerSummonMinionOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        is ArenaMessageState.Teleport -> MageTeleportOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsFor = avatarBounds::boundsFor,
            modifier = modifier,
        )
        else -> {}
    }
}


// TODO: Need to do something different for Attack vs Outcome???
@Composable
private fun AnimateNormalAttack(
    animatingMessage: ArenaMessageState.NormalRoll,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    val attackerBounds =
        avatarBoundsStateHolder.boundsFor(userId = animatingMessage.attackerUserId) ?: return
    val defenderBounds =
        avatarBoundsStateHolder.boundsFor(userId = animatingMessage.defenderUserId) ?: return

    when (animatingMessage.attackerClassType) {
        CharacterClassType.Rogue ->
            RogueNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        CharacterClassType.Gambler -> GamblerNormalAttackOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Duelist -> DuelistNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Necromancer -> NecromancerNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Bard -> BardNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Illusionist -> IllusionistNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Mage -> MageNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.Paladin -> PaladinNormalAttackOverlay(
            animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
            modifier = modifier,
        )
        CharacterClassType.None -> {
            // TODO: Should never happen - maybe need a new type or something?
        }
    }
}

