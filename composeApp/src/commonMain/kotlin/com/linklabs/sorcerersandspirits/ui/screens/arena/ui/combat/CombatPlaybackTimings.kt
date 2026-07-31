package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatAnimationCue
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

internal fun CombatAnimationCue.playbackDurationMillis(): Long =
    maxOf(a = effectOverlayDurationMillis(), b = seatedAvatarDurationMillis())

private fun CombatAnimationCue.effectOverlayDurationMillis(): Long =
    when (this) {
        is CombatAnimationCue.NormalAttack ->
            normalAttackOverlayDurationMillis(
                weaponClassType = weaponClassType,
                normalRollMessageType = normalRollMessageType,
            )

        is CombatAnimationCue.KillingMomentum -> 0L

        is CombatAnimationCue.Dodge -> 0L

        is CombatAnimationCue.AllIn -> gamblerAllInDurationMillis(outcome = outcome)

        is CombatAnimationCue.LuckyCoinFlip ->
            gamblerLuckyCoinFlipDurationMillis(didStopAttack = didStopAttack)

        is CombatAnimationCue.LuckyCoinHighRoller -> gamblerLuckyCoinHighRollerDurationMillis()

        is CombatAnimationCue.LuckyCoinHighRollerThrow -> gamblerLuckyCoinThrowDurationMillis()

        is CombatAnimationCue.PrepareAttack -> PREPARE_ATTACK_TOTAL_MILLIS

        is CombatAnimationCue.Riposte ->
            when (phase) {
                RipostePhase.Ready -> RIPOSTE_READY_MILLIS

                RipostePhase.Landed,
                RipostePhase.Missed -> RIPOSTE_TOTAL_MILLIS
            }

        is CombatAnimationCue.SummonMinion ->
            necromancerSummonMinionDurationMillis(outcome = outcome)

        is CombatAnimationCue.Persuade -> persuadeDurationMillis(phase = phase)

        is CombatAnimationCue.Encore -> encoreDurationMillis(phase = phase)

        is CombatAnimationCue.Impersonate -> illusionistImpersonateDurationMillis()

        is CombatAnimationCue.MirrorImage -> 0L

        is CombatAnimationCue.SpellPotency ->
            mageSpellPotencyDurationMillis(mainDamage = mainDamage)

        is CombatAnimationCue.Teleport ->
            teleportOverlayDurationMillis(phase = phase, didEscape = didEscape)

        is CombatAnimationCue.HolyShield ->
            paladinHolyShieldOverlayDurationMillis(phase = phase, didSurvive = didSurvive)
    }

private fun normalAttackOverlayDurationMillis(
    weaponClassType: CharacterClassType,
    normalRollMessageType: NormalRollMessageType,
): Long =
    when (weaponClassType) {
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
