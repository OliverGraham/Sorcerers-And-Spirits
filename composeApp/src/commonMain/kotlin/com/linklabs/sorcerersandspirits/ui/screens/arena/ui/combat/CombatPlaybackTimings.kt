package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RipostePhase
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.weaponClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.bardNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.encoreDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.bard.persuadeDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.PREPARE_ATTACK_TOTAL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_READY_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.RIPOSTE_TOTAL_MILLIS
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.duelist.duelistNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerAllInDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinFlipDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinHighRollerDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerLuckyCoinThrowDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.gambler.gamblerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.illusionist.illusionistImpersonateDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.mageNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.mageSpellPotencyDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.mage.teleportOverlayDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer.necromancerSummonMinionDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.paladinHolyShieldOverlayDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.paladin.paladinNormalAttackDurationMillis
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.rogue.rogueNormalAttackDurationMillis

internal const val FLASH_HOLD_MILLIS = 1100L
internal const val FLASH_EXIT_MILLIS = 320L

internal const val FLASH_MESSAGE_DURATION_MILLIS = FLASH_HOLD_MILLIS + FLASH_EXIT_MILLIS

internal fun ArenaMessageState.playbackDurationMillis(): Long =
    maxOf(a = effectOverlayDurationMillis(), b = seatedAvatarDurationMillis())

private fun ArenaMessageState.effectOverlayDurationMillis(): Long =
    when (this) {
        is ArenaMessageState.NormalRoll ->
            normalAttackOverlayDurationMillis(
                weaponClassType = weaponClassType,
                normalRollMessageType = normalRollMessageType,
            )

        is ArenaMessageState.KillingMomentum -> 0L

        is ArenaMessageState.Dodge -> 0L

        is ArenaMessageState.AllIn -> gamblerAllInDurationMillis(outcome = outcome)

        is ArenaMessageState.LuckyCoinFlip ->
            gamblerLuckyCoinFlipDurationMillis(didStopAttack = didStopAttack)

        is ArenaMessageState.LuckyCoinHighRoller -> gamblerLuckyCoinHighRollerDurationMillis()

        is ArenaMessageState.LuckyCoinHighRollerThrow -> gamblerLuckyCoinThrowDurationMillis()

        is ArenaMessageState.PrepareAttack -> PREPARE_ATTACK_TOTAL_MILLIS

        is ArenaMessageState.Riposte ->
            when (phase) {
                RipostePhase.Ready -> RIPOSTE_READY_MILLIS

                RipostePhase.Landed,
                RipostePhase.Missed -> RIPOSTE_TOTAL_MILLIS
            }

        is ArenaMessageState.SummonMinion ->
            necromancerSummonMinionDurationMillis(outcome = outcome)

        is ArenaMessageState.Persuade -> persuadeDurationMillis(phase = phase)

        is ArenaMessageState.Encore -> encoreDurationMillis(phase = phase)

        is ArenaMessageState.Impersonate -> illusionistImpersonateDurationMillis()

        is ArenaMessageState.MirrorImage -> 0L

        is ArenaMessageState.SpellPotency -> mageSpellPotencyDurationMillis(mainDamage = mainDamage)

        is ArenaMessageState.Teleport ->
            teleportOverlayDurationMillis(phase = phase, didEscape = didEscape)

        is ArenaMessageState.HolyShield ->
            paladinHolyShieldOverlayDurationMillis(phase = phase, didSurvive = didSurvive)
    }

private fun normalAttackOverlayDurationMillis(
    characterClassType: CharacterClassType,
    normalRollMessageType: NormalRollMessageType,
): Long =
    when (characterClassType) {
        CharacterClassType.Rogue ->
            rogueNormalAttackDurationMillis(normalRollMessageType = normalRollMessageType)

        CharacterClassType.Gambler -> gamblerNormalAttackDurationMillis()

        CharacterClassType.Duelist -> duelistNormalAttackDurationMillis()

        CharacterClassType.Necromancer -> necromancerNormalAttackDurationMillis()

        CharacterClassType.Bard -> bardNormalAttackDurationMillis()

        CharacterClassType.Mage ->
            mageNormalAttackDurationMillis(normalRollMessageType = normalRollMessageType)

        CharacterClassType.Paladin -> paladinNormalAttackDurationMillis()

        else -> 0L
    }
