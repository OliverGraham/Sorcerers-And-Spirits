package com.linklabs.sorcerersandspirits.ui.common.characterclasses

// TODO(SS-081): These stat/ability descriptions are static literals shared by the class wheel and
//  the GameRulesScreen. SS-081 moves them to RemoteConfig with long (rules) + short (in-UI)
// variants.

internal const val HEALTH_DESCRIPTION =
    "Hit points. When a damage roll meets or beats this, you're knocked out."
internal const val ARMOR_DESCRIPTION =
    "Armor Class. An attacker's first roll must meet or beat this to land a hit."
internal const val SPEED_DESCRIPTION = "Sets turn order — higher speed acts earlier in the round."

internal fun offensiveAbilityName(ability: OffensiveAbilityState): String =
    when (ability) {
        is OffensiveAbilityState.KillingMomentum -> "Killing Momentum"
        is OffensiveAbilityState.AllIn -> "All-In"
        is OffensiveAbilityState.PrepareAttack -> "Prepare Attack"
        is OffensiveAbilityState.SummonMinion -> "Summon Minion"
        is OffensiveAbilityState.Encore -> "Encore"
        is OffensiveAbilityState.Impersonate -> "Impersonate"
        is OffensiveAbilityState.SpellPotency -> "Spell Potency"
        is OffensiveAbilityState.None -> "None"
    }

internal fun offensiveAbilityDescription(ability: OffensiveAbilityState): String =
    when (ability) {
        is OffensiveAbilityState.KillingMomentum ->
            "If you knock out an opponent, immediately attack another player of your choice."

        is OffensiveAbilityState.AllIn ->
            "Activated. Roll 1d6: on 5–6 attack twice this round; on 1 you knock yourself out; 2–4 no effect."

        is OffensiveAbilityState.PrepareAttack ->
            "Activated. Roll 2d6 — a 10, 11 or 12 knocks the target out."

        is OffensiveAbilityState.SummonMinion ->
            "Activated. Roll 1d6 to raise a skeleton or golem (and sometimes heal). Minions add a shield and an extra attack."

        is OffensiveAbilityState.Encore ->
            "After dealing damage, roll 1d6 — on a 5 or 6, attack again."

        is OffensiveAbilityState.Impersonate ->
            "Activated. Copy a living opponent's offensive ability until you choose another."

        is OffensiveAbilityState.SpellPotency ->
            "Roll doubles to hit or damage: deal the dice total to the target and half to every other opponent, then end your turn."

        is OffensiveAbilityState.None -> ""
    }

internal fun defensiveAbilityName(ability: DefensiveAbilityState): String =
    when (ability) {
        is DefensiveAbilityState.Dodge -> "Dodge"
        is DefensiveAbilityState.LuckyCoin -> "Lucky Coin"
        is DefensiveAbilityState.Riposte -> "Riposte"
        is DefensiveAbilityState.Persuade -> "Persuade"
        is DefensiveAbilityState.MirrorImage -> "Mirror Image"
        is DefensiveAbilityState.Teleport -> "Teleport"
        is DefensiveAbilityState.HolyShield -> "Holy Shield"
        is DefensiveAbilityState.None -> "None"
    }

internal fun defensiveAbilityDescription(ability: DefensiveAbilityState): String =
    when (ability) {
        is DefensiveAbilityState.Dodge ->
            "Each time an attacker fails to break your armor, gain +1 Armor (up to +2) for the match."

        is DefensiveAbilityState.LuckyCoin ->
            "When your armor is broken, add 1d6 to Armor until turn end; on a 6, deal 1d6 back to the attacker."

        is DefensiveAbilityState.Riposte ->
            "When an opponent passes your armor, roll 2d6 — on a 6, 7 or 8 you turn the attack back on them."

        is DefensiveAbilityState.Persuade ->
            "When your armor is passed, roll 1d6 — on a 5 stop the attack; on a 6 redirect it to another target."

        is DefensiveAbilityState.MirrorImage ->
            "Once per attack phase, reroll one die and subtract 1 (result can be 0). Not usable on special-ability damage."

        is DefensiveAbilityState.Teleport ->
            "When your armor is passed, roll off 1d6 with the attacker — roll higher to escape the attack."

        is DefensiveAbilityState.HolyShield ->
            "Once per match, when knocked out roll 2d6 — on 10–12 survive at 1 HP and deal 1d6 unblockable to the attacker."

        is DefensiveAbilityState.None -> ""
    }
