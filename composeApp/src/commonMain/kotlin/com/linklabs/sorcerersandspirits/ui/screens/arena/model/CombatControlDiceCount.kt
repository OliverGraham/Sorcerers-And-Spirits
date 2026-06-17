package com.linklabs.sorcerersandspirits.ui.screens.arena.model

/**
 * How many dice the current step rolls, driving how many are held for the shake-to-roll gesture
 * (SS-069). `null` marks a control that resolves a choice rather than a roll (Impersonate copies an
 * ability; Charm picks a target), so it stays a plain button instead of a held-dice gesture.
 */
internal val ArenaCombatControl.diceCount: Int?
    get() =
        when (this) {
            is ArenaCombatControl.NormalAttack -> TWO_DICE
            is ArenaCombatControl.ActivatedAbility -> action.diceCount
            is ArenaCombatControl.AbilityRoll -> action.diceCount
        }

private const val ONE_DIE = 1
private const val TWO_DICE = 2

private val AbilityActionState.diceCount: Int?
    get() =
        when (this) {
            is AbilityActionState.Activated.AllIn -> ONE_DIE
            is AbilityActionState.Activated.SummonMinion -> ONE_DIE
            is AbilityActionState.Activated.PrepareAttack -> TWO_DICE
            is AbilityActionState.Activated.Impersonate -> null
            is AbilityActionState.Passive.LuckyCoin -> ONE_DIE
            is AbilityActionState.Passive.Riposte -> TWO_DICE
            is AbilityActionState.Passive.Persuade.StrumLute -> ONE_DIE
            is AbilityActionState.Passive.Persuade.Charm -> null
            is AbilityActionState.Passive.Encore -> ONE_DIE
            is AbilityActionState.Passive.Teleport -> ONE_DIE
            is AbilityActionState.Passive.MirrorImage -> ONE_DIE
            is AbilityActionState.Passive.HolyShield.Prayer -> TWO_DICE
            is AbilityActionState.Passive.HolyShield.Wrath -> ONE_DIE
        }
