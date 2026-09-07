package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.SummonMinionOutcome
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_message_all_in_double_attack
import sorcerersandspirits.composeapp.generated.resources.arena_message_all_in_no_effect
import sorcerersandspirits.composeapp.generated.resources.arena_message_all_in_self_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_chose_character_class
import sorcerersandspirits.composeapp.generated.resources.arena_message_dodge_armor_increased
import sorcerersandspirits.composeapp.generated.resources.arena_message_dodge_armor_unchanged
import sorcerersandspirits.composeapp.generated.resources.arena_message_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_message_encore_roll_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_encore_roll_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_encore_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_holy_shield_prayer_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_holy_shield_prayer_survived
import sorcerersandspirits.composeapp.generated.resources.arena_message_holy_shield_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_holy_shield_wrath
import sorcerersandspirits.composeapp.generated.resources.arena_message_holy_shield_wrath_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_impersonate
import sorcerersandspirits.composeapp.generated.resources.arena_message_joined_arena
import sorcerersandspirits.composeapp.generated.resources.arena_message_killing_momentum
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_flip_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_flip_stopped
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_minion_attack
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_pass_armor_broke
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_pass_armor_held
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_pass_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_pass_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_tempt_fate_armor_broke
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_tempt_fate_armor_held
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_tempt_fate_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_tempt_fate_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_mirror_image_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_new_turn
import sorcerersandspirits.composeapp.generated.resources.arena_message_next_match_starting_soon
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_hit
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_miss
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_charm
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_charm_activation
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_strum_lute_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_strum_lute_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_prepare_attack_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_prepare_attack_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_readied_for_next_match
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_en_garde_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_en_garde_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_round
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency_no_splash
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency_no_splash_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_fizzle
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_golem
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_skeleton
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_skeleton_heal
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_escaped
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_roll
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_tie
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_trigger

// TODO: Can add resource to data class?
@Composable
internal fun ArenaMessageState.getDisplayString(): String = when (this) {
    is ArenaMessageState.NewRound ->
        stringResource(resource = Res.string.arena_message_round, roundNumber)

    is ArenaMessageState.ChoseCharacterClass ->
        stringResource(
            resource = Res.string.arena_message_chose_character_class,
            displayName,
            characterClassInfo.className,
        )

    is ArenaMessageState.ConnectedToArena ->
        stringResource(resource = Res.string.arena_message_joined_arena, displayName)

    is ArenaMessageState.ReadiedForNextMatch ->
        stringResource(resource = Res.string.arena_message_readied_for_next_match, displayName)

    is ArenaMessageState.NextMatchStartingSoon ->
        stringResource(resource = Res.string.arena_message_next_match_starting_soon)

    is ArenaMessageState.NewTurn ->
        stringResource(
            resource = Res.string.arena_message_new_turn,
            characterClassInfo.className,
        )

    is ArenaMessageState.NormalRoll.Attack ->
        if (minionAttackerName != null) {
            stringResource(
                resource = Res.string.arena_message_minion_attack,
                attackerClassName,
                minionAttackerName,
            )
        } else {
            ""
        }

    is ArenaMessageState.NormalRoll.Outcome ->
        when (normalRollMessageType) {
            NormalRollMessageType.ArmorClassHit ->
                stringResource(
                    resource = Res.string.arena_message_normal_roll_hit,
                    attackerDisplayName,
                    defenderClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = bonus),
                    defenderArmorClass,
                )

            NormalRollMessageType.ArmorClassMiss ->
                stringResource(
                    resource = Res.string.arena_message_normal_roll_miss,
                    attackerDisplayName,
                    defenderClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = bonus),
                    defenderArmorClass,
                )

            NormalRollMessageType.Damage ->
                stringResource(
                    resource = Res.string.arena_message_normal_roll_damage,
                    attackerDisplayName,
                    defenderClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = bonus),
                )

            NormalRollMessageType.Knockout ->
                stringResource(
                    resource = Res.string.arena_message_normal_roll_knockout,
                    attackerDisplayName,
                    defenderClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = bonus),
                )
        }

    is ArenaMessageState.KillingMomentum ->
        stringResource(resource = Res.string.arena_message_killing_momentum)

    is ArenaMessageState.Dodge ->
        if (didArmorIncrease) {
            stringResource(
                resource = Res.string.arena_message_dodge_armor_increased,
                newArmorClass,
                armorBonus,
                maxArmorBonus,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_dodge_armor_unchanged,
                maxArmorBonus,
                newArmorClass,
            )
        }

    is ArenaMessageState.Teleport.Trigger ->
        stringResource(resource = Res.string.arena_message_teleport_trigger)

    is ArenaMessageState.Teleport.Roll ->
        stringResource(resource = Res.string.arena_message_teleport_roll, className, singleDie)

    is ArenaMessageState.Teleport.Tie ->
        stringResource(resource = Res.string.arena_message_teleport_tie)

    is ArenaMessageState.Teleport.Result ->
        if (didDefenderEscape) {
            stringResource(resource = Res.string.arena_message_teleport_escaped)
        } else {
            stringResource(resource = Res.string.arena_message_teleport_failed)
        }

    is ArenaMessageState.LuckyCoin.Trigger ->
        stringResource(
            resource = Res.string.arena_message_lucky_coin_trigger,
            minimumCoinToStop,
            attackerClassName,
            attackerRollTotal,
        )

    is ArenaMessageState.LuckyCoin.Flip ->
        if (didStopAttack) {
            stringResource(
                resource = Res.string.arena_message_lucky_coin_flip_stopped,
                singleDie,
                defenderArmorClass,
                attackerClassName,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_lucky_coin_flip_failed,
                singleDie,
                defenderArmorClass,
                attackerClassName,
            )
        }

    is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
        stringResource(
            resource = Res.string.arena_message_lucky_coin_high_roller_trigger,
            attackerClassName,
        )

    is ArenaMessageState.LuckyCoin.HighRollerDamage ->
        if (isKnockout) {
            stringResource(
                resource = Res.string.arena_message_lucky_coin_high_roller_knockout,
                attackerClassName,
                singleDie,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_lucky_coin_high_roller_damage,
                attackerClassName,
                singleDie,
            )
        }

    is ArenaMessageState.Riposte.Trigger ->
        stringResource(
            resource = Res.string.arena_message_riposte_trigger,
            attackerClassName,
            successRangeMin,
            successRangeMax,
        )

    is ArenaMessageState.Riposte.EnGarde ->
        if (isSuccessful) {
            stringResource(
                resource = Res.string.arena_message_riposte_en_garde_success,
                firstDie,
                secondDie,
                total,
                attackerClassName,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_riposte_en_garde_failed,
                firstDie,
                secondDie,
                total,
                attackerClassName,
            )
        }

    is ArenaMessageState.SummonMinion ->
        when (outcome) {
            SummonMinionOutcome.Fizzle ->
                stringResource(
                    resource = Res.string.arena_message_summon_minion_fizzle,
                    className,
                    singleDie,
                )

            SummonMinionOutcome.Skeleton ->
                stringResource(
                    resource = Res.string.arena_message_summon_minion_skeleton,
                    className,
                    singleDie,
                    minionName.orEmpty(),
                )

            SummonMinionOutcome.SkeletonAndHeal ->
                stringResource(
                    resource = Res.string.arena_message_summon_minion_skeleton_heal,
                    className,
                    singleDie,
                    minionName.orEmpty(),
                )

            SummonMinionOutcome.Golem ->
                stringResource(
                    resource = Res.string.arena_message_summon_minion_golem,
                    className,
                    singleDie,
                    minionName.orEmpty(),
                )
        }

    is ArenaMessageState.Persuade.Trigger ->
        stringResource(resource = Res.string.arena_message_persuade_trigger)

    is ArenaMessageState.Persuade.StrumLute ->
        if (isSuccessful) {
            stringResource(
                resource = Res.string.arena_message_persuade_strum_lute_success,
                diceRoll,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_persuade_strum_lute_failed,
                diceRoll,
            )
        }

    is ArenaMessageState.Persuade.CharmActivation ->
        stringResource(resource = Res.string.arena_message_persuade_charm_activation)

    is ArenaMessageState.Persuade.Charm ->
        stringResource(
            resource = Res.string.arena_message_persuade_charm,
            attackerClassName,
            defenderClassName,
        )

    is ArenaMessageState.Encore.Trigger ->
        stringResource(
            resource = Res.string.arena_message_encore_trigger,
            successRangeMin,
            successRangeMax,
        )

    is ArenaMessageState.Encore.Roll ->
        if (isSuccessful) {
            stringResource(resource = Res.string.arena_message_encore_roll_success, encoreDie)
        } else {
            stringResource(resource = Res.string.arena_message_encore_roll_failed, encoreDie)
        }

    is ArenaMessageState.Impersonate ->
        stringResource(
            resource = Res.string.arena_message_impersonate,
            illusionistName,
            impersonatedAbilityName,
        )

    is ArenaMessageState.MirrorImage.Trigger ->
        stringResource(resource = Res.string.arena_message_mirror_image_trigger)

    is ArenaMessageState.MirrorImage.Pass ->
        when (messageType) {
            NormalRollMessageType.ArmorClassHit ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_pass_armor_broke,
                    attackerClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = doublesBonus),
                )

            NormalRollMessageType.ArmorClassMiss ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_pass_armor_held,
                    attackerClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = doublesBonus),
                )

            NormalRollMessageType.Damage ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_pass_damage,
                    attackerClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = doublesBonus),
                )

            NormalRollMessageType.Knockout ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_pass_knockout,
                    attackerClassName,
                    rolledTotalDisplay(baseRoll = baseRoll, doublesBonus = doublesBonus),
                )
        }

    is ArenaMessageState.MirrorImage.TemptFate ->
        when (messageType) {
            NormalRollMessageType.ArmorClassHit ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_tempt_fate_armor_broke,
                    rerollDisplay,
                    attackerClassName,
                )

            NormalRollMessageType.ArmorClassMiss ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_tempt_fate_armor_held,
                    rerollDisplay,
                    attackerClassName,
                )

            NormalRollMessageType.Damage ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_tempt_fate_damage,
                    rerollDisplay,
                    attackerClassName,
                )

            NormalRollMessageType.Knockout ->
                stringResource(
                    resource = Res.string.arena_message_mirror_image_tempt_fate_knockout,
                    rerollDisplay,
                    attackerClassName,
                )
        }

    is ArenaMessageState.SpellPotency ->
        if (splashDamage > 0) {
            if (isMainTargetKnockedOut) {
                stringResource(
                    resource = Res.string.arena_message_spell_potency_knockout,
                    mageName,
                    mainTargetClassName,
                    mainDamage,
                    splashDamage,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_spell_potency,
                    mageName,
                    mainTargetClassName,
                    mainDamage,
                    splashDamage,
                )
            }
        } else {
            if (isMainTargetKnockedOut) {
                stringResource(
                    resource = Res.string.arena_message_spell_potency_no_splash_knockout,
                    mageName,
                    mainTargetClassName,
                    mainDamage,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_spell_potency_no_splash,
                    mageName,
                    mainTargetClassName,
                    mainDamage,
                )
            }
        }

    is ArenaMessageState.HolyShield.Trigger ->
        stringResource(
            resource = Res.string.arena_message_holy_shield_trigger,
            successRangeMin,
            successRangeMax,
        )

    is ArenaMessageState.HolyShield.Prayer ->
        if (didSurvive) {
            stringResource(
                resource = Res.string.arena_message_holy_shield_prayer_survived,
                firstDie,
                secondDie,
                total,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_holy_shield_prayer_failed,
                firstDie,
                secondDie,
                total,
            )
        }

    is ArenaMessageState.HolyShield.Wrath ->
        if (isAttackerKnockedOut) {
            stringResource(
                resource = Res.string.arena_message_holy_shield_wrath_knockout,
                paladinName,
                attackerClassName,
                singleDie,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_holy_shield_wrath,
                paladinName,
                attackerClassName,
                singleDie,
            )
        }

    is ArenaMessageState.AllIn ->
        when (outcome) {
            AllInOutcome.SelfKnockout ->
                stringResource(
                    resource = Res.string.arena_message_all_in_self_knockout,
                    gamblerName,
                    singleDie,
                )

            AllInOutcome.NoEffect ->
                stringResource(
                    resource = Res.string.arena_message_all_in_no_effect,
                    gamblerName,
                    singleDie,
                )

            AllInOutcome.DoubleAttack ->
                stringResource(
                    resource = Res.string.arena_message_all_in_double_attack,
                    gamblerName,
                    singleDie,
                )
        }

    is ArenaMessageState.Doubles ->
        stringResource(resource = Res.string.arena_message_doubles, className)

    is ArenaMessageState.PrepareAttack.Strike -> ""

    is ArenaMessageState.PrepareAttack.Outcome ->
        if (isSuccessful) {
            stringResource(
                resource = Res.string.arena_message_prepare_attack_success,
                duelistName,
                firstDie,
                secondDie,
                total,
                knockoutRangeMin,
                knockoutRangeMax,
                targetClassName,
            )
        } else {
            stringResource(
                resource = Res.string.arena_message_prepare_attack_failed,
                duelistName,
                firstDie,
                secondDie,
                total,
                knockoutRangeMin,
                knockoutRangeMax,
                targetClassName,
            )
        }
}

private fun rolledTotalDisplay(baseRoll: Int, doublesBonus: Int) = if (doublesBonus >
    0
) {
    "$baseRoll (+$doublesBonus)"
} else {
    baseRoll.toString()
}
