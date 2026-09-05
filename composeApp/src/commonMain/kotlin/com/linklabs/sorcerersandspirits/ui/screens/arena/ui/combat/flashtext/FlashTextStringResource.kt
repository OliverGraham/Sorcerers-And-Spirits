package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.flashtext

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.SummonMinionOutcome
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_flash_all_in_no_effect
import sorcerersandspirits.composeapp.generated.resources.arena_flash_all_in_strike_twice
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_continues
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_stopped
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_whiffed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_caught
import sorcerersandspirits.composeapp.generated.resources.arena_flash_charmed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_chose_class
import sorcerersandspirits.composeapp.generated.resources.arena_flash_counter_failed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_damage
import sorcerersandspirits.composeapp.generated.resources.arena_flash_damage_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_dodge
import sorcerersandspirits.composeapp.generated.resources.arena_flash_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_encore
import sorcerersandspirits.composeapp.generated.resources.arena_flash_encore_again
import sorcerersandspirits.composeapp.generated.resources.arena_flash_escaped
import sorcerersandspirits.composeapp.generated.resources.arena_flash_fizzle
import sorcerersandspirits.composeapp.generated.resources.arena_flash_golem
import sorcerersandspirits.composeapp.generated.resources.arena_flash_high_roller
import sorcerersandspirits.composeapp.generated.resources.arena_flash_hit
import sorcerersandspirits.composeapp.generated.resources.arena_flash_hit_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_holy_shield
import sorcerersandspirits.composeapp.generated.resources.arena_flash_holy_wrath
import sorcerersandspirits.composeapp.generated.resources.arena_flash_impersonate
import sorcerersandspirits.composeapp.generated.resources.arena_flash_joined
import sorcerersandspirits.composeapp.generated.resources.arena_flash_killing_momentum
import sorcerersandspirits.composeapp.generated.resources.arena_flash_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_flash_knockout_total
import sorcerersandspirits.composeapp.generated.resources.arena_flash_knockout_total_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lucky_coin
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lucky_coin_continues
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lucky_coin_stopped
import sorcerersandspirits.composeapp.generated.resources.arena_flash_mirror_holds
import sorcerersandspirits.composeapp.generated.resources.arena_flash_mirror_image
import sorcerersandspirits.composeapp.generated.resources.arena_flash_miss
import sorcerersandspirits.composeapp.generated.resources.arena_flash_miss_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_new_round
import sorcerersandspirits.composeapp.generated.resources.arena_flash_no_encore
import sorcerersandspirits.composeapp.generated.resources.arena_flash_persuade
import sorcerersandspirits.composeapp.generated.resources.arena_flash_prepared_strike
import sorcerersandspirits.composeapp.generated.resources.arena_flash_redirected
import sorcerersandspirits.composeapp.generated.resources.arena_flash_reroll
import sorcerersandspirits.composeapp.generated.resources.arena_flash_reversed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_riposte
import sorcerersandspirits.composeapp.generated.resources.arena_flash_saved
import sorcerersandspirits.composeapp.generated.resources.arena_flash_skeleton
import sorcerersandspirits.composeapp.generated.resources.arena_flash_skeleton_and_heal
import sorcerersandspirits.composeapp.generated.resources.arena_flash_spell_potency
import sorcerersandspirits.composeapp.generated.resources.arena_flash_struck_down
import sorcerersandspirits.composeapp.generated.resources.arena_flash_teleport
import sorcerersandspirits.composeapp.generated.resources.arena_flash_teleport_roll
import sorcerersandspirits.composeapp.generated.resources.arena_flash_teleport_tie
import sorcerersandspirits.composeapp.generated.resources.arena_flash_turn_start

@Composable
internal fun ArenaMessageState.toStringResource(): String = when (this) {
    is ArenaMessageState.NormalRoll.Outcome ->
        when (normalRollMessageType) {
            NormalRollMessageType.ArmorClassHit ->
                if (bonus > 0) {
                    stringResource(
                        resource = Res.string.arena_flash_hit_doubles,
                        baseRoll,
                        bonus,
                    )
                } else {
                    stringResource(resource = Res.string.arena_flash_hit, baseRoll)
                }

            NormalRollMessageType.ArmorClassMiss ->
                if (bonus > 0) {
                    stringResource(
                        resource = Res.string.arena_flash_miss_doubles,
                        baseRoll,
                        bonus,
                    )
                } else {
                    stringResource(resource = Res.string.arena_flash_miss, baseRoll)
                }

            NormalRollMessageType.Damage ->
                if (bonus > 0) {
                    stringResource(
                        resource = Res.string.arena_flash_damage_doubles,
                        baseRoll,
                        bonus,
                    )
                } else {
                    stringResource(resource = Res.string.arena_flash_damage, baseRoll)
                }

            NormalRollMessageType.Knockout ->
                if (bonus > 0) {
                    stringResource(
                        resource = Res.string.arena_flash_knockout_total_doubles,
                        baseRoll,
                        bonus,
                    )
                } else {
                    stringResource(resource = Res.string.arena_flash_knockout_total, baseRoll)
                }
        }

    is ArenaMessageState.NormalRoll.Attack -> ""

    is ArenaMessageState.KillingMomentum ->
        stringResource(resource = Res.string.arena_flash_killing_momentum)

    is ArenaMessageState.Dodge -> stringResource(resource = Res.string.arena_flash_dodge)

    is ArenaMessageState.SpellPotency ->
        stringResource(resource = Res.string.arena_flash_spell_potency)

    is ArenaMessageState.Doubles -> stringResource(resource = Res.string.arena_flash_doubles)

    is ArenaMessageState.Impersonate ->
        stringResource(resource = Res.string.arena_flash_impersonate)

    is ArenaMessageState.PrepareAttack.Strike -> ""

    is ArenaMessageState.PrepareAttack.Outcome ->
        if (isSuccessful) {
            stringResource(resource = Res.string.arena_flash_prepared_strike)
        } else {
            stringResource(resource = Res.string.arena_flash_attack_whiffed)
        }

    is ArenaMessageState.Encore.Trigger ->
        stringResource(resource = Res.string.arena_flash_encore)

    is ArenaMessageState.Encore.Roll ->
        if (isSuccessful) {
            stringResource(resource = Res.string.arena_flash_encore_again)
        } else {
            stringResource(resource = Res.string.arena_flash_no_encore)
        }

    is ArenaMessageState.AllIn ->
        when (outcome) {
            AllInOutcome.DoubleAttack ->
                stringResource(resource = Res.string.arena_flash_all_in_strike_twice)

            AllInOutcome.NoEffect ->
                stringResource(resource = Res.string.arena_flash_all_in_no_effect)

            AllInOutcome.SelfKnockout ->
                stringResource(resource = Res.string.arena_flash_knockout)
        }

    is ArenaMessageState.SummonMinion ->
        when (outcome) {
            SummonMinionOutcome.Fizzle ->
                stringResource(resource = Res.string.arena_flash_fizzle)

            SummonMinionOutcome.Skeleton ->
                stringResource(resource = Res.string.arena_flash_skeleton)

            SummonMinionOutcome.SkeletonAndHeal ->
                stringResource(resource = Res.string.arena_flash_skeleton_and_heal)

            SummonMinionOutcome.Golem -> stringResource(resource = Res.string.arena_flash_golem)
        }

    is ArenaMessageState.Teleport.Trigger ->
        stringResource(resource = Res.string.arena_flash_teleport)

    is ArenaMessageState.Teleport.Result ->
        if (didDefenderEscape) {
            stringResource(resource = Res.string.arena_flash_escaped)
        } else {
            stringResource(resource = Res.string.arena_flash_caught)
        }

    is ArenaMessageState.Teleport.Roll ->
        stringResource(resource = Res.string.arena_flash_teleport_roll, singleDie)

    is ArenaMessageState.Teleport.Tie ->
        stringResource(resource = Res.string.arena_flash_teleport_tie)

    is ArenaMessageState.LuckyCoin.Trigger ->
        stringResource(resource = Res.string.arena_flash_lucky_coin, minimumCoinToStop)

    is ArenaMessageState.LuckyCoin.Flip ->
        if (didStopAttack) {
            stringResource(resource = Res.string.arena_flash_lucky_coin_stopped, singleDie)
        } else {
            stringResource(resource = Res.string.arena_flash_lucky_coin_continues, singleDie)
        }

    is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
        stringResource(resource = Res.string.arena_flash_high_roller)

    is ArenaMessageState.LuckyCoin.HighRollerDamage ->
        if (isKnockout) {
            stringResource(resource = Res.string.arena_flash_knockout)
        } else {
            stringResource(resource = Res.string.arena_flash_damage, singleDie)
        }

    is ArenaMessageState.Riposte.Trigger ->
        stringResource(resource = Res.string.arena_flash_riposte)

    is ArenaMessageState.Riposte.EnGarde ->
        if (isSuccessful) {
            stringResource(resource = Res.string.arena_flash_reversed)
        } else {
            stringResource(resource = Res.string.arena_flash_counter_failed)
        }

    is ArenaMessageState.Persuade.Trigger ->
        stringResource(resource = Res.string.arena_flash_persuade)

    is ArenaMessageState.Persuade.StrumLute ->
        if (isSuccessful) {
            stringResource(resource = Res.string.arena_flash_attack_stopped)
        } else {
            stringResource(resource = Res.string.arena_flash_attack_continues, diceRoll)
        }

    is ArenaMessageState.Persuade.CharmActivation ->
        stringResource(resource = Res.string.arena_flash_charmed)

    is ArenaMessageState.Persuade.Charm ->
        stringResource(resource = Res.string.arena_flash_redirected, defenderClassName)

    is ArenaMessageState.MirrorImage.Trigger ->
        stringResource(resource = Res.string.arena_flash_mirror_image)

    is ArenaMessageState.MirrorImage.TemptFate ->
        stringResource(resource = Res.string.arena_flash_reroll, rawReroll)

    is ArenaMessageState.MirrorImage.Pass ->
        if (messageType == NormalRollMessageType.Knockout) {
            if (doublesBonus > 0) {
                stringResource(
                    resource = Res.string.arena_flash_knockout_total_doubles,
                    baseRoll,
                    doublesBonus,
                )
            } else {
                stringResource(resource = Res.string.arena_flash_knockout_total, baseRoll)
            }
        } else {
            stringResource(resource = Res.string.arena_flash_mirror_holds)
        }

    is ArenaMessageState.HolyShield.Trigger ->
        stringResource(resource = Res.string.arena_flash_holy_shield)

    is ArenaMessageState.HolyShield.Prayer ->
        if (didSurvive) {
            stringResource(resource = Res.string.arena_flash_saved)
        } else {
            stringResource(resource = Res.string.arena_flash_struck_down)
        }

    is ArenaMessageState.HolyShield.Wrath ->
        stringResource(resource = Res.string.arena_flash_holy_wrath)

    is ArenaMessageState.NewRound ->
        stringResource(resource = Res.string.arena_flash_new_round, roundNumber)

    is ArenaMessageState.ChoseCharacterClass ->
        stringResource(
            resource = Res.string.arena_flash_chose_class,
            characterClassInfo.className,
        )

    is ArenaMessageState.ConnectedToArena ->
        stringResource(resource = Res.string.arena_flash_joined, displayName)

    is ArenaMessageState.ReadiedForNextMatch -> ""

    is ArenaMessageState.NextMatchStartingSoon -> ""

    is ArenaMessageState.NewTurn ->
        stringResource(
            resource = Res.string.arena_flash_turn_start,
            characterClassInfo.className,
        )
}
