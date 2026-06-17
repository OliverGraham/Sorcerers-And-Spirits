package com.linklabs.sorcerersandspirits.ui.screens.arena.animation

import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState

/**
 * Maps an [ArenaMessageState] to the [CombatAnimation] that should play for it, or `null` when the
 * event is purely textual (lobby/round/turn banners) and needs no combat visual.
 *
 * This is the single source of truth for "what does this action look like". It is a pure function
 * so it can be unit-tested and previewed, and it is exhaustive over every message leaf for the same
 * reason `getDisplayString` is: every conceivable action is guaranteed a defined visual.
 */
internal fun ArenaMessageState.toCombatAnimation(): CombatAnimation? =
    when (this) {
        // --- Purely textual events: no combat visual ---
        is ArenaMessageState.NewRound,
        is ArenaMessageState.ChoseCharacterClass,
        is ArenaMessageState.ConnectedToArena,
        is ArenaMessageState.NewTurn -> null

        // --- Normal attack ---
        is ArenaMessageState.NormalRoll ->
            when (type) {
                NormalRollMessageType.ArmorClassHit ->
                    CombatAnimation(
                        fx = CombatFx.Slash,
                        attackerGesture = AvatarGesture.Lunge,
                        defenderGesture = AvatarGesture.Recoil,
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    CombatAnimation(fx = CombatFx.ArmorBlock, attackerGesture = AvatarGesture.Lunge)

                NormalRollMessageType.Damage ->
                    CombatAnimation(
                        fx = CombatFx.Impact,
                        attackerGesture = AvatarGesture.Lunge,
                        defenderGesture = AvatarGesture.Recoil,
                    )

                NormalRollMessageType.Knockout ->
                    CombatAnimation(
                        fx = CombatFx.Knockout,
                        attackerGesture = AvatarGesture.Lunge,
                        defenderGesture = AvatarGesture.Collapse,
                    )
            }

        // --- Rogue ---
        is ArenaMessageState.KillingMomentum ->
            CombatAnimation(fx = CombatFx.Slash, attackerGesture = AvatarGesture.Cheer)

        is ArenaMessageState.Dodge ->
            CombatAnimation(fx = CombatFx.Evade, defenderGesture = AvatarGesture.Recoil)

        // --- Mage ---
        is ArenaMessageState.Teleport.Trigger ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.Teleport.Roll -> CombatAnimation(fx = CombatFx.DiceRoll)

        is ArenaMessageState.Teleport.Result ->
            if (didDefenderEscape) {
                CombatAnimation(fx = CombatFx.Evade, defenderGesture = AvatarGesture.Cast)
            } else {
                CombatAnimation(fx = CombatFx.Impact, defenderGesture = AvatarGesture.Recoil)
            }

        // --- Gambler ---
        is ArenaMessageState.LuckyCoin.Trigger ->
            CombatAnimation(fx = CombatFx.Shield, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.LuckyCoin.Flip -> CombatAnimation(fx = CombatFx.DiceRoll)

        is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cheer)

        is ArenaMessageState.LuckyCoin.HighRollerDamage ->
            if (isKnockout) {
                CombatAnimation(fx = CombatFx.Knockout, attackerGesture = AvatarGesture.Collapse)
            } else {
                CombatAnimation(fx = CombatFx.Impact, attackerGesture = AvatarGesture.Recoil)
            }

        is ArenaMessageState.AllIn -> CombatAnimation(fx = CombatFx.DiceRoll)

        // --- Duelist ---
        is ArenaMessageState.Riposte.Trigger ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.Riposte.EnGarde ->
            if (isSuccessful) {
                CombatAnimation(fx = CombatFx.Slash, defenderGesture = AvatarGesture.Lunge)
            } else {
                CombatAnimation(fx = CombatFx.Miss)
            }

        is ArenaMessageState.PrepareAttack ->
            if (isSuccessful) {
                CombatAnimation(
                    fx = CombatFx.Knockout,
                    attackerGesture = AvatarGesture.Lunge,
                    defenderGesture = AvatarGesture.Collapse,
                )
            } else {
                CombatAnimation(fx = CombatFx.Miss, attackerGesture = AvatarGesture.Lunge)
            }

        // --- Necromancer ---
        is ArenaMessageState.SummonMinion ->
            CombatAnimation(fx = CombatFx.Summon, attackerGesture = AvatarGesture.Cast)

        // --- Bard ---
        is ArenaMessageState.Persuade.Trigger ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.Persuade.StrumLute -> CombatAnimation(fx = CombatFx.DiceRoll)

        is ArenaMessageState.Persuade.CharmActivation ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cheer)

        is ArenaMessageState.Persuade.Lull ->
            CombatAnimation(fx = CombatFx.Shield, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.Persuade.Charm ->
            CombatAnimation(fx = CombatFx.MagicBurst, attackerGesture = AvatarGesture.Cast)

        is ArenaMessageState.Encore.Trigger ->
            CombatAnimation(fx = CombatFx.Cast, attackerGesture = AvatarGesture.Cheer)

        is ArenaMessageState.Encore.Roll ->
            if (isSuccessful) {
                CombatAnimation(fx = CombatFx.Slash, attackerGesture = AvatarGesture.Lunge)
            } else {
                CombatAnimation(fx = CombatFx.DiceRoll)
            }

        // --- Illusionist ---
        is ArenaMessageState.Impersonate ->
            CombatAnimation(fx = CombatFx.MagicBurst, attackerGesture = AvatarGesture.Cast)

        is ArenaMessageState.MirrorImage.Trigger ->
            CombatAnimation(fx = CombatFx.Cast, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.MirrorImage.Pass -> outcome.toDefenderOutcomeAnimation()

        is ArenaMessageState.MirrorImage.TemptFate -> outcome.toDefenderOutcomeAnimation()

        // --- Mage offensive ---
        is ArenaMessageState.SpellPotency ->
            CombatAnimation(
                fx = CombatFx.MagicBurst,
                attackerGesture = AvatarGesture.Cast,
                defenderGesture = AvatarGesture.Recoil,
            )

        // --- Paladin ---
        is ArenaMessageState.HolyShield.Trigger ->
            CombatAnimation(fx = CombatFx.Shield, defenderGesture = AvatarGesture.Cast)

        is ArenaMessageState.HolyShield.Prayer ->
            if (didSurvive) {
                CombatAnimation(fx = CombatFx.Shield, defenderGesture = AvatarGesture.Cheer)
            } else {
                CombatAnimation(fx = CombatFx.Knockout, defenderGesture = AvatarGesture.Collapse)
            }

        is ArenaMessageState.HolyShield.Wrath ->
            CombatAnimation(
                fx = CombatFx.Impact,
                defenderGesture = AvatarGesture.Cast,
                attackerGesture = AvatarGesture.Recoil,
            )
    }

/** Shared outcome → visual for "an attack resolved against the defender" (used by MirrorImage). */
private fun NormalRollMessageType.toDefenderOutcomeAnimation(): CombatAnimation =
    when (this) {
        NormalRollMessageType.ArmorClassHit ->
            CombatAnimation(fx = CombatFx.Slash, defenderGesture = AvatarGesture.Recoil)

        NormalRollMessageType.ArmorClassMiss -> CombatAnimation(fx = CombatFx.ArmorBlock)

        NormalRollMessageType.Damage ->
            CombatAnimation(fx = CombatFx.Impact, defenderGesture = AvatarGesture.Recoil)

        NormalRollMessageType.Knockout ->
            CombatAnimation(fx = CombatFx.Knockout, defenderGesture = AvatarGesture.Collapse)
    }
