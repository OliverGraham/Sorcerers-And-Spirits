package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.WeaponType
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
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.GolemStoneFistOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.MinionDeathOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerNormalAttackOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.NecromancerSummonMinionOverlay
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.ScythePalette
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.SkeletonLongSwordOverlay
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
    when (animatingMessage) {
        is ArenaMessageState.NormalRoll.Attack ->
            AnimateNormalAttack(
                animatingMessage = animatingMessage,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                modifier = modifier,
            )

        is ArenaMessageState.KillingMomentum -> {
            // TODO(SS-258): should probably be something
        }

        is ArenaMessageState.Dodge -> {
            // TODO(SS-258): no sure where this one is. So weird
        }

        is ArenaMessageState.AllIn -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.gamblerUserId) ?: return

            AllInOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                accentColor = animatingMessage.actingClassType.toColor(),
                modifier = modifier,
            )
        }

        is ArenaMessageState.LuckyCoin ->
            LuckyCoinOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
                modifier = modifier,
            )

        is ArenaMessageState.PrepareAttack.Strike -> {
            val duelistBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.duelistUserId) ?: return
            val targetBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.targetUserId) ?: return

            PrepareAttackOverlay(
                animatingMessage = animatingMessage,
                duelistBounds = duelistBounds,
                targetBounds = targetBounds,
                accentColor = animatingMessage.actingClassType.toColor(),
                modifier = modifier,
            )
        }

        is ArenaMessageState.Riposte -> {
            val duelistBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.duelistUserId) ?: return
            val attackerBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.attackerUserId)
                    ?: return

            RiposteOverlay(
                riposteMessage = animatingMessage,
                duelistBounds = duelistBounds,
                attackerBounds = attackerBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.SummonMinion.Summon -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.necromancerUserId)
                    ?: return

            NecromancerSummonMinionOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.SummonMinion.MinionDamaged -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.summonerUserId)
                    ?: return

            MinionDeathOverlay(
                messageId = animatingMessage.id,
                minionType = animatingMessage.minionType,
                minionIndex = animatingMessage.minionIndex,
                avatarBounds = avatarBounds,
                accentColor = animatingMessage.actingClassType.toColor(),
                modifier = modifier,
            )
        }

        is ArenaMessageState.SummonMinion.MinionDestroyed -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.summonerUserId)
                    ?: return

            MinionDeathOverlay(
                messageId = animatingMessage.id,
                minionType = animatingMessage.minionType,
                minionIndex = animatingMessage.minionIndex,
                avatarBounds = avatarBounds,
                accentColor = animatingMessage.actingClassType.toColor(),
                modifier = modifier,
            )
        }

        is ArenaMessageState.Encore ->
            EncoreOverlay(
                animatingMessage = animatingMessage,
                accentColor = animatingMessage.actingClassType.toColor(),
                modifier = modifier,
            )

        is ArenaMessageState.Persuade -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.bardUserId) ?: return

            PersuadeOverlay(
                animatingMessage = animatingMessage,
                bardBounds = avatarBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.Impersonate -> {
            val avatarBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.illusionistUserId)
                    ?: return

            ImpersonateOverlay(
                animatingMessage = animatingMessage,
                avatarBounds = avatarBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.MirrorImage -> {
            // TODO(SS-258): Where is it?
        }

        is ArenaMessageState.SpellPotency -> {
            val mageBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.mageUserId) ?: return
            val defenderBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.mainTargetUserId)
                    ?: return

            MageSpellPotencyOverlay(
                animatingMessage = animatingMessage,
                mageBounds = mageBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.Teleport -> {
            val mageBounds =
                avatarBoundsStateHolder.boundsFor(userId = animatingMessage.mageUserId) ?: return

            MageTeleportOverlay(
                animatingMessage = animatingMessage,
                mageBounds = mageBounds,
                modifier = modifier,
            )
        }

        is ArenaMessageState.HolyShield ->
            PaladinHolyShieldOverlay(
                animatingMessage = animatingMessage,
                avatarBoundsStateHolder = avatarBoundsStateHolder,
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
) {
    when (animatingMessage.weaponType) {
        WeaponType.Dagger ->
            RogueNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.Card ->
            GamblerNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.Rapiers ->
            DuelistNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.Scythe ->
            NecromancerNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                scythePalette = animatingMessage.scythePalette(),
                modifier = modifier,
            )

        WeaponType.Drum ->
            BardNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.Spell ->
            MageNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.Mace ->
            PaladinNormalAttackOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.LongSword ->
            SkeletonLongSwordOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )

        WeaponType.StoneFist ->
            GolemStoneFistOverlay(
                animatingMessage = animatingMessage,
                attackerBounds = attackerBounds,
                defenderBounds = defenderBounds,
                modifier = modifier,
            )
    }
}

// TODO: Don't convert to color and then check - just first then convert if necessary.
//       Pass info from message itself - no workarounds like this.
private fun ArenaMessageState.NormalRoll.Attack.scythePalette(): ScythePalette {
    val isIllusionistScythe = accentColor == CharacterClassType.Illusionist.toColor()
    return if (isIllusionistScythe) illusionistScythePalette else necroScythePalette
}
