package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.BardNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.EncoreOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.PersuadeOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.DuelistNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PrepareAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RiposteOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.AllInOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.GamblerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.LuckyCoinOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.ImpersonateOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageSpellPotencyOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.MageTeleportOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerSummonMinionOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.ScythePalette
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.illusionistScythePalette
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necroScythePalette
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
        is ArenaMessageState.NormalRoll.Attack -> AnimateNormalAttack(
            animatingMessage = animatingMessage,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            modifier = modifier,
        )

        is ArenaMessageState.KillingMomentum -> {}
        is ArenaMessageState.Dodge -> {
            // TODO: no sure where this one is. So weird
        }
        is ArenaMessageState.AllIn -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.gamblerUserId) ?: return

            AllInOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                modifier = modifier,
            )
        }
        is ArenaMessageState.LuckyCoin -> LuckyCoinOverlay(
            animatingMessage = animatingMessage,
            avatarBoundsStateHolder = avatarBoundsStateHolder,
            modifier = modifier,
        )
        is ArenaMessageState.PrepareAttack -> {
            val duelistBounds = avatarBoundsStateHolder.boundsFor(userId = animatingMessage.duelistUserId) ?: return
            val targetBounds = avatarBoundsStateHolder.boundsFor(userId = animatingMessage.targetUserId) ?: return

            PrepareAttackOverlay(
                animatingMessage = animatingMessage,
                duelistBounds = duelistBounds,
                targetBounds = targetBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.Riposte -> {
            val duelistBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.duelistUserId) ?: return
            val attackerBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.attackerUserId) ?: return

            RiposteOverlay(
                riposteMessage = animatingMessage,
                duelistBounds = duelistBounds,
                attackerBounds = attackerBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.SummonMinion -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.necromancerUserId) ?: return

            NecromancerSummonMinionOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                modifier = modifier,
            )
        }
        is ArenaMessageState.Encore ->
            EncoreOverlay(animatingMessage = animatingMessage, modifier = modifier)
        is ArenaMessageState.Persuade -> PersuadeOverlay(
            animatingMessage = animatingMessage,
            avatarBounds = avatarBounds,
            modifier = modifier,
        )
        is ArenaMessageState.Impersonate -> ImpersonateOverlay(
            animatingMessage = animatingMessage,
            avatarBounds = avatarBounds,
            modifier = modifier,
        )

        is ArenaMessageState.MirrorImage -> {
            // TODO: Where is it?
        }
        is ArenaMessageState.SpellPotency ->
            // TODO: attacker and defender bounds
            MageSpellPotencyOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )


        is ArenaMessageState.Teleport -> MageTeleportOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            modifier = modifier,
        )

        is ArenaMessageState.HolyShield -> PaladinHolyShieldOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            modifier = modifier,
        )
        else -> {}
    }
}

@Composable
private fun AnimateNormalAttack(
    animatingMessage: ArenaMessageState.NormalRoll.Attack,
    avatarBoundsStateHolder: AvatarBoundsStateHolder,
    modifier: Modifier = Modifier,
) {
    val attackerBounds =
        avatarBoundsStateHolder.boundsFor(userId = animatingMessage.attackerUserId) ?: return
    val defenderBounds =
        avatarBoundsStateHolder.boundsFor(userId = animatingMessage.defenderUserId) ?: return

    AnimateNormalAttack(
        animatingMessage = animatingMessage,
        attackerBounds = attackerBounds,
        defenderBounds = defenderBounds,
        modifier = modifier,
    )
}

@Composable
private fun AnimateNormalAttack(
    animatingMessage: ArenaMessageState.NormalRoll.Attack,
    attackerBounds: Rect,
    defenderBounds: Rect,
    modifier: Modifier = Modifier,
    attackerClassType: CharacterClassType = animatingMessage.attackerClassType,
    accentColor: Color = attackerClassType.toColor(),
    scythePalette: ScythePalette = necroScythePalette,
) {
    when (attackerClassType) {
        CharacterClassType.Rogue ->
            RogueNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                accentColor = accentColor,
                modifier = modifier,
            )

        CharacterClassType.Gambler -> GamblerNormalAttackOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            accentColor = accentColor,
            modifier = modifier,
        )
        CharacterClassType.Duelist -> DuelistNormalAttackOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            accentColor = accentColor,
            modifier = modifier,
        )
        CharacterClassType.Necromancer -> NecromancerNormalAttackOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            scythePalette = scythePalette,
            modifier = modifier,
        )
        CharacterClassType.Bard -> BardNormalAttackOverlay(
            animatingMessage = animatingMessage,
            attackerBounds = attackerBounds,
            defenderBounds = defenderBounds,
            accentColor = accentColor,
            modifier = modifier,
        )
        CharacterClassType.Illusionist ->
            AnimateNormalAttack(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
                attackerClassType = animatingMessage.getImpersonatedAttackTypeOrRandom(),
                scythePalette = illusionistScythePalette,
                accentColor = CharacterClassType.Illusionist.toColor(),
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
            accentColor = accentColor,
            modifier = modifier,
        )
        CharacterClassType.None -> {
            // TODO: Should never happen - maybe need a new type or something?
        }
    }
}

private fun ArenaMessageState.NormalRoll.Attack.getImpersonatedAttackTypeOrRandom() =
    if (impersonatedClassType == null || impersonatedClassType == CharacterClassType.Illusionist) {
        CharacterClassType.entries.filterNot {
            it == CharacterClassType.Illusionist || it == CharacterClassType.None
        }.random()
    } else {
        impersonatedClassType
    }
