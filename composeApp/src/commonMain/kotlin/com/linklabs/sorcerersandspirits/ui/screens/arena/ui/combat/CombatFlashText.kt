package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
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
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lulled
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
internal fun FlashCallout.text(): String =
    when (this) {
        is FlashCallout.Hit ->
            if (doublesBonus > 0) {
                stringResource(
                    resource = Res.string.arena_flash_hit_doubles,
                    baseRoll,
                    doublesBonus,
                )
            } else {
                stringResource(resource = Res.string.arena_flash_hit, baseRoll)
            }

        is FlashCallout.Miss ->
            if (doublesBonus > 0) {
                stringResource(
                    resource = Res.string.arena_flash_miss_doubles,
                    baseRoll,
                    doublesBonus,
                )
            } else {
                stringResource(resource = Res.string.arena_flash_miss, baseRoll)
            }

        is FlashCallout.Damage ->
            if (doublesBonus > 0) {
                stringResource(
                    resource = Res.string.arena_flash_damage_doubles,
                    baseRoll,
                    doublesBonus,
                )
            } else {
                stringResource(resource = Res.string.arena_flash_damage, baseRoll)
            }

        is FlashCallout.Knockout ->
            if (baseRoll == null) {
                stringResource(resource = Res.string.arena_flash_knockout)
            } else if (doublesBonus > 0) {
                stringResource(
                    resource = Res.string.arena_flash_knockout_total_doubles,
                    baseRoll,
                    doublesBonus,
                )
            } else {
                stringResource(resource = Res.string.arena_flash_knockout_total, baseRoll)
            }

        is FlashCallout.KillingMomentum ->
            stringResource(resource = Res.string.arena_flash_killing_momentum)

        is FlashCallout.Dodge -> stringResource(resource = Res.string.arena_flash_dodge)

        is FlashCallout.SpellPotency ->
            stringResource(resource = Res.string.arena_flash_spell_potency)

        is FlashCallout.Doubles -> stringResource(resource = Res.string.arena_flash_doubles)

        is FlashCallout.Impersonate -> stringResource(resource = Res.string.arena_flash_impersonate)

        is FlashCallout.PreparedStrike ->
            stringResource(resource = Res.string.arena_flash_prepared_strike)

        is FlashCallout.AttackWhiffed ->
            stringResource(resource = Res.string.arena_flash_attack_whiffed)

        is FlashCallout.Encore -> stringResource(resource = Res.string.arena_flash_encore)

        is FlashCallout.AllInNoEffect ->
            stringResource(resource = Res.string.arena_flash_all_in_no_effect)

        is FlashCallout.AllInStrikeTwice ->
            stringResource(resource = Res.string.arena_flash_all_in_strike_twice)

        is FlashCallout.Fizzle -> stringResource(resource = Res.string.arena_flash_fizzle)

        is FlashCallout.Skeleton -> stringResource(resource = Res.string.arena_flash_skeleton)

        is FlashCallout.SkeletonAndHeal ->
            stringResource(resource = Res.string.arena_flash_skeleton_and_heal)

        is FlashCallout.Golem -> stringResource(resource = Res.string.arena_flash_golem)

        is FlashCallout.Teleport -> stringResource(resource = Res.string.arena_flash_teleport)

        is FlashCallout.Escaped -> stringResource(resource = Res.string.arena_flash_escaped)

        is FlashCallout.Caught -> stringResource(resource = Res.string.arena_flash_caught)

        is FlashCallout.LuckyCoin -> stringResource(resource = Res.string.arena_flash_lucky_coin)

        is FlashCallout.AttackStopped ->
            stringResource(resource = Res.string.arena_flash_attack_stopped)

        is FlashCallout.AttackContinues ->
            stringResource(resource = Res.string.arena_flash_attack_continues, die)

        is FlashCallout.Riposte -> stringResource(resource = Res.string.arena_flash_riposte)

        is FlashCallout.Reversed -> stringResource(resource = Res.string.arena_flash_reversed)

        is FlashCallout.CounterFailed ->
            stringResource(resource = Res.string.arena_flash_counter_failed)

        is FlashCallout.Persuade -> stringResource(resource = Res.string.arena_flash_persuade)

        is FlashCallout.Charmed -> stringResource(resource = Res.string.arena_flash_charmed)

        is FlashCallout.Redirected ->
            stringResource(resource = Res.string.arena_flash_redirected, targetName)

        is FlashCallout.MirrorImage ->
            stringResource(resource = Res.string.arena_flash_mirror_image)

        is FlashCallout.Reroll ->
            stringResource(resource = Res.string.arena_flash_reroll, rawReroll)

        is FlashCallout.HolyShield -> stringResource(resource = Res.string.arena_flash_holy_shield)

        is FlashCallout.Saved -> stringResource(resource = Res.string.arena_flash_saved)

        is FlashCallout.StruckDown -> stringResource(resource = Res.string.arena_flash_struck_down)

        is FlashCallout.HolyWrath -> stringResource(resource = Res.string.arena_flash_holy_wrath)

        is FlashCallout.NewRound ->
            stringResource(resource = Res.string.arena_flash_new_round, roundNumber)

        is FlashCallout.ChoseClass ->
            stringResource(resource = Res.string.arena_flash_chose_class, className)

        is FlashCallout.Joined ->
            stringResource(resource = Res.string.arena_flash_joined, playerName)

        is FlashCallout.TurnStart ->
            stringResource(resource = Res.string.arena_flash_turn_start, className)

        is FlashCallout.TeleportRoll ->
            stringResource(resource = Res.string.arena_flash_teleport_roll, die)

        is FlashCallout.TieRollAgain ->
            stringResource(resource = Res.string.arena_flash_teleport_tie)

        is FlashCallout.HighRoller -> stringResource(resource = Res.string.arena_flash_high_roller)

        is FlashCallout.Lulled -> stringResource(resource = Res.string.arena_flash_lulled)

        is FlashCallout.EncoreAgain ->
            stringResource(resource = Res.string.arena_flash_encore_again)

        is FlashCallout.NoEncore -> stringResource(resource = Res.string.arena_flash_no_encore)

        is FlashCallout.MirrorHolds ->
            stringResource(resource = Res.string.arena_flash_mirror_holds)
    }
