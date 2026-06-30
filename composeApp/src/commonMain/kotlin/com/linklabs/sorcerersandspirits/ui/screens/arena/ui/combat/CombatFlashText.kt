package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Composable
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.FlashCallout
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_flash_all_in
import sorcerersandspirits.composeapp.generated.resources.arena_flash_all_in_strike_twice
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_continues
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_stopped
import sorcerersandspirits.composeapp.generated.resources.arena_flash_attack_whiffed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_caught
import sorcerersandspirits.composeapp.generated.resources.arena_flash_charmed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_chose_class
import sorcerersandspirits.composeapp.generated.resources.arena_flash_counter_failed
import sorcerersandspirits.composeapp.generated.resources.arena_flash_damage
import sorcerersandspirits.composeapp.generated.resources.arena_flash_dodge
import sorcerersandspirits.composeapp.generated.resources.arena_flash_doubles
import sorcerersandspirits.composeapp.generated.resources.arena_flash_encore
import sorcerersandspirits.composeapp.generated.resources.arena_flash_encore_again
import sorcerersandspirits.composeapp.generated.resources.arena_flash_escaped
import sorcerersandspirits.composeapp.generated.resources.arena_flash_fizzle
import sorcerersandspirits.composeapp.generated.resources.arena_flash_golem
import sorcerersandspirits.composeapp.generated.resources.arena_flash_high_roller
import sorcerersandspirits.composeapp.generated.resources.arena_flash_hit
import sorcerersandspirits.composeapp.generated.resources.arena_flash_holy_shield
import sorcerersandspirits.composeapp.generated.resources.arena_flash_holy_wrath
import sorcerersandspirits.composeapp.generated.resources.arena_flash_impersonate
import sorcerersandspirits.composeapp.generated.resources.arena_flash_joined
import sorcerersandspirits.composeapp.generated.resources.arena_flash_killing_momentum
import sorcerersandspirits.composeapp.generated.resources.arena_flash_knockout
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lucky_coin
import sorcerersandspirits.composeapp.generated.resources.arena_flash_lulled
import sorcerersandspirits.composeapp.generated.resources.arena_flash_mirror_holds
import sorcerersandspirits.composeapp.generated.resources.arena_flash_mirror_image
import sorcerersandspirits.composeapp.generated.resources.arena_flash_miss
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
import sorcerersandspirits.composeapp.generated.resources.arena_flash_turn_start

/** The rendered wording for a [FlashCallout], built from its string resource and data (SS-164). */
@Composable
internal fun FlashCallout.text(): String =
    when (this) {
        is FlashCallout.Hit -> stringResource(Res.string.arena_flash_hit, total)

        is FlashCallout.Miss -> stringResource(Res.string.arena_flash_miss)

        is FlashCallout.Damage -> stringResource(Res.string.arena_flash_damage, amount)

        is FlashCallout.Knockout -> stringResource(Res.string.arena_flash_knockout)

        is FlashCallout.KillingMomentum -> stringResource(Res.string.arena_flash_killing_momentum)

        is FlashCallout.Dodge -> stringResource(Res.string.arena_flash_dodge)

        is FlashCallout.SpellPotency -> stringResource(Res.string.arena_flash_spell_potency)

        is FlashCallout.Doubles -> stringResource(Res.string.arena_flash_doubles)

        is FlashCallout.Impersonate -> stringResource(Res.string.arena_flash_impersonate)

        is FlashCallout.PreparedStrike -> stringResource(Res.string.arena_flash_prepared_strike)

        is FlashCallout.AttackWhiffed -> stringResource(Res.string.arena_flash_attack_whiffed)

        is FlashCallout.Encore -> stringResource(Res.string.arena_flash_encore)

        is FlashCallout.AllIn -> stringResource(Res.string.arena_flash_all_in)

        is FlashCallout.AllInStrikeTwice ->
            stringResource(Res.string.arena_flash_all_in_strike_twice)

        is FlashCallout.Fizzle -> stringResource(Res.string.arena_flash_fizzle)

        is FlashCallout.Skeleton -> stringResource(Res.string.arena_flash_skeleton)

        is FlashCallout.SkeletonAndHeal ->
            stringResource(Res.string.arena_flash_skeleton_and_heal, necromancerName)

        is FlashCallout.Golem -> stringResource(Res.string.arena_flash_golem, necromancerName)

        is FlashCallout.Teleport -> stringResource(Res.string.arena_flash_teleport)

        is FlashCallout.Escaped -> stringResource(Res.string.arena_flash_escaped)

        is FlashCallout.Caught -> stringResource(Res.string.arena_flash_caught)

        is FlashCallout.LuckyCoin -> stringResource(Res.string.arena_flash_lucky_coin)

        is FlashCallout.AttackStopped -> stringResource(Res.string.arena_flash_attack_stopped)

        is FlashCallout.AttackContinues ->
            stringResource(Res.string.arena_flash_attack_continues, die)

        is FlashCallout.Riposte -> stringResource(Res.string.arena_flash_riposte)

        is FlashCallout.Reversed -> stringResource(Res.string.arena_flash_reversed)

        is FlashCallout.CounterFailed -> stringResource(Res.string.arena_flash_counter_failed)

        is FlashCallout.Persuade -> stringResource(Res.string.arena_flash_persuade)

        is FlashCallout.Charmed -> stringResource(Res.string.arena_flash_charmed)

        is FlashCallout.Redirected -> stringResource(Res.string.arena_flash_redirected, targetName)

        is FlashCallout.MirrorImage -> stringResource(Res.string.arena_flash_mirror_image)

        is FlashCallout.Reroll -> stringResource(Res.string.arena_flash_reroll, finalRoll)

        is FlashCallout.HolyShield -> stringResource(Res.string.arena_flash_holy_shield)

        is FlashCallout.Saved -> stringResource(Res.string.arena_flash_saved)

        is FlashCallout.StruckDown -> stringResource(Res.string.arena_flash_struck_down)

        is FlashCallout.HolyWrath -> stringResource(Res.string.arena_flash_holy_wrath)

        is FlashCallout.NewRound -> stringResource(Res.string.arena_flash_new_round, roundNumber)

        is FlashCallout.ChoseClass -> stringResource(Res.string.arena_flash_chose_class, className)

        is FlashCallout.Joined -> stringResource(Res.string.arena_flash_joined, playerName)

        is FlashCallout.TurnStart -> stringResource(Res.string.arena_flash_turn_start, className)

        is FlashCallout.TeleportRoll -> stringResource(Res.string.arena_flash_teleport_roll, die)

        is FlashCallout.HighRoller -> stringResource(Res.string.arena_flash_high_roller)

        is FlashCallout.Lulled -> stringResource(Res.string.arena_flash_lulled)

        is FlashCallout.EncoreAgain -> stringResource(Res.string.arena_flash_encore_again)

        is FlashCallout.NoEncore -> stringResource(Res.string.arena_flash_no_encore)

        is FlashCallout.MirrorHolds -> stringResource(Res.string.arena_flash_mirror_holds)
    }
