package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.message

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
import sorcerersandspirits.composeapp.generated.resources.arena_message_impersonate
import sorcerersandspirits.composeapp.generated.resources.arena_message_joined_arena
import sorcerersandspirits.composeapp.generated.resources.arena_message_killing_momentum
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_flip_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_flip_stopped
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_high_roller_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_lucky_coin_trigger
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
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_damage
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_hit
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_message_normal_roll_miss
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_charm
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_charm_activation
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_lull
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_strum_lute_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_strum_lute_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_persuade_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_prepare_attack_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_prepare_attack_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_en_garde_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_en_garde_success
import sorcerersandspirits.composeapp.generated.resources.arena_message_riposte_trigger
import sorcerersandspirits.composeapp.generated.resources.arena_message_round
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency
import sorcerersandspirits.composeapp.generated.resources.arena_message_spell_potency_no_splash
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_fizzle
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_golem
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_skeleton
import sorcerersandspirits.composeapp.generated.resources.arena_message_summon_minion_skeleton_heal
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_escaped
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_failed
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_roll
import sorcerersandspirits.composeapp.generated.resources.arena_message_teleport_trigger

@Composable
internal fun ArenaMessageState.getDisplayString(): String =
    when (this) {
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

        is ArenaMessageState.NewTurn ->
            stringResource(
                resource = Res.string.arena_message_new_turn,
                characterClassInfo.className,
            )

        is ArenaMessageState.NormalRoll ->
            when (type) {
                NormalRollMessageType.ArmorClassHit ->
                    stringResource(
                        resource = Res.string.arena_message_normal_roll_hit,
                        attackerName,
                        defenderName,
                        rolledTotalDisplay(total = total, doublesBonus = bonus),
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    stringResource(
                        resource = Res.string.arena_message_normal_roll_miss,
                        attackerName,
                        defenderName,
                        rolledTotalDisplay(total = total, doublesBonus = bonus),
                    )

                NormalRollMessageType.Damage ->
                    stringResource(
                        resource = Res.string.arena_message_normal_roll_damage,
                        attackerName,
                        defenderName,
                        rolledTotalDisplay(total = total, doublesBonus = bonus),
                    )

                NormalRollMessageType.Knockout ->
                    stringResource(
                        resource = Res.string.arena_message_normal_roll_knockout,
                        attackerName,
                        defenderName,
                        rolledTotalDisplay(total = total, doublesBonus = bonus),
                    )
            }

        is ArenaMessageState.KillingMomentum ->
            stringResource(resource = Res.string.arena_message_killing_momentum)

        is ArenaMessageState.Dodge ->
            if (didArmorIncrease) {
                stringResource(resource = Res.string.arena_message_dodge_armor_increased)
            } else {
                stringResource(resource = Res.string.arena_message_dodge_armor_unchanged)
            }

        is ArenaMessageState.Teleport.Trigger ->
            stringResource(resource = Res.string.arena_message_teleport_trigger)

        is ArenaMessageState.Teleport.Roll ->
            stringResource(resource = Res.string.arena_message_teleport_roll, playerName, singleDie)

        is ArenaMessageState.Teleport.Result ->
            if (didDefenderEscape) {
                stringResource(resource = Res.string.arena_message_teleport_escaped)
            } else {
                stringResource(resource = Res.string.arena_message_teleport_failed)
            }

        is ArenaMessageState.LuckyCoin.Trigger ->
            stringResource(resource = Res.string.arena_message_lucky_coin_trigger)

        is ArenaMessageState.LuckyCoin.Flip ->
            if (didStopAttack) {
                stringResource(
                    resource = Res.string.arena_message_lucky_coin_flip_stopped,
                    singleDie,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_lucky_coin_flip_failed,
                    singleDie,
                )
            }

        is ArenaMessageState.LuckyCoin.HighRollerTrigger ->
            stringResource(
                resource = Res.string.arena_message_lucky_coin_high_roller_trigger,
                targetName,
            )

        is ArenaMessageState.LuckyCoin.HighRollerDamage ->
            if (isKnockout) {
                stringResource(
                    resource = Res.string.arena_message_lucky_coin_high_roller_knockout,
                    singleDie,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_lucky_coin_high_roller_damage,
                    singleDie,
                )
            }

        is ArenaMessageState.Riposte.Trigger ->
            stringResource(resource = Res.string.arena_message_riposte_trigger)

        is ArenaMessageState.Riposte.EnGarde ->
            if (isSuccessful) {
                stringResource(
                    resource = Res.string.arena_message_riposte_en_garde_success,
                    firstDie,
                    secondDie,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_riposte_en_garde_failed,
                    firstDie,
                    secondDie,
                )
            }

        is ArenaMessageState.SummonMinion ->
            when (outcome) {
                SummonMinionOutcome.Fizzle ->
                    stringResource(
                        resource = Res.string.arena_message_summon_minion_fizzle,
                        necroName,
                        singleDie,
                    )

                SummonMinionOutcome.Skeleton ->
                    stringResource(
                        resource = Res.string.arena_message_summon_minion_skeleton,
                        necroName,
                        singleDie,
                    )

                SummonMinionOutcome.SkeletonAndHeal ->
                    stringResource(
                        resource = Res.string.arena_message_summon_minion_skeleton_heal,
                        necroName,
                        singleDie,
                    )

                SummonMinionOutcome.Golem ->
                    stringResource(
                        resource = Res.string.arena_message_summon_minion_golem,
                        necroName,
                        singleDie,
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

        is ArenaMessageState.Persuade.Lull ->
            stringResource(resource = Res.string.arena_message_persuade_lull)

        is ArenaMessageState.Persuade.Charm ->
            stringResource(
                resource = Res.string.arena_message_persuade_charm,
                attackerName,
                defenderName,
            )

        is ArenaMessageState.Encore.Trigger ->
            stringResource(resource = Res.string.arena_message_encore_trigger)

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
            when (outcome) {
                NormalRollMessageType.ArmorClassHit ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_pass_armor_broke,
                        attackerName,
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_pass_armor_held,
                        attackerName,
                    )

                NormalRollMessageType.Damage ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_pass_damage,
                        attackerName,
                    )

                NormalRollMessageType.Knockout ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_pass_knockout,
                        attackerName,
                    )
            }

        is ArenaMessageState.MirrorImage.TemptFate ->
            when (outcome) {
                NormalRollMessageType.ArmorClassHit ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_tempt_fate_armor_broke,
                        rerollDisplay,
                        attackerName,
                    )

                NormalRollMessageType.ArmorClassMiss ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_tempt_fate_armor_held,
                        rerollDisplay,
                        attackerName,
                    )

                NormalRollMessageType.Damage ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_tempt_fate_damage,
                        rerollDisplay,
                        attackerName,
                    )

                NormalRollMessageType.Knockout ->
                    stringResource(
                        resource = Res.string.arena_message_mirror_image_tempt_fate_knockout,
                        rerollDisplay,
                        attackerName,
                    )
            }

        is ArenaMessageState.SpellPotency ->
            if (splashDamage > 0) {
                stringResource(
                    resource = Res.string.arena_message_spell_potency,
                    mageName,
                    mainTargetName,
                    mainDamage,
                    splashDamage,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_spell_potency_no_splash,
                    mageName,
                    mainTargetName,
                    mainDamage,
                )
            }

        is ArenaMessageState.HolyShield.Trigger ->
            stringResource(resource = Res.string.arena_message_holy_shield_trigger)

        is ArenaMessageState.HolyShield.Prayer ->
            if (didSurvive) {
                stringResource(
                    resource = Res.string.arena_message_holy_shield_prayer_survived,
                    firstDie,
                    secondDie,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_holy_shield_prayer_failed,
                    firstDie,
                    secondDie,
                )
            }

        is ArenaMessageState.HolyShield.Wrath ->
            stringResource(
                resource = Res.string.arena_message_holy_shield_wrath,
                paladinName,
                attackerName,
                singleDie,
            )

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
            stringResource(resource = Res.string.arena_message_doubles, playerName)

        is ArenaMessageState.PrepareAttack ->
            if (isSuccessful) {
                stringResource(
                    resource = Res.string.arena_message_prepare_attack_success,
                    duelistName,
                    targetName,
                    firstDie,
                    secondDie,
                )
            } else {
                stringResource(
                    resource = Res.string.arena_message_prepare_attack_failed,
                    duelistName,
                    targetName,
                    firstDie,
                    secondDie,
                )
            }
    }

private fun rolledTotalDisplay(total: Int, doublesBonus: Int): String =
    if (doublesBonus > 0) "$total (+$doublesBonus)" else total.toString()
