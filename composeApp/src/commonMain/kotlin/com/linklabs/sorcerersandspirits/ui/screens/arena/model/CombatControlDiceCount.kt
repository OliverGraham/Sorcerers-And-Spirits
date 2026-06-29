package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction

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

private val FIRST_SLOT = setOf(0)
private val SECOND_SLOT = setOf(1)
private val BOTH_SLOTS = setOf(0, 1)

/**
 * Which physical dice the current step rolls. Most rolls throw the first die (or both), but Mirror
 * Image rerolls exactly the die the Illusionist chose, leaving the other untouched (SS-166). `null`
 * marks a control that resolves a choice rather than a roll.
 */
internal val ArenaCombatControl.diceSlots: Set<Int>?
    get() =
        when (this) {
            is ArenaCombatControl.NormalAttack -> BOTH_SLOTS
            is ArenaCombatControl.ActivatedAbility -> action.diceSlots
            is ArenaCombatControl.AbilityRoll -> action.diceSlots
        }

private val AbilityActionState.diceSlots: Set<Int>?
    get() =
        when (this) {
            is AbilityActionState.Passive.MirrorImage ->
                when (rerollSelection) {
                    AbilityAction.Passive.MirrorImage.RerollSelection.First -> FIRST_SLOT
                    AbilityAction.Passive.MirrorImage.RerollSelection.Second -> SECOND_SLOT
                    AbilityAction.Passive.MirrorImage.RerollSelection.None -> null
                }

            else ->
                when (diceCount) {
                    ONE_DIE -> FIRST_SLOT
                    TWO_DICE -> BOTH_SLOTS
                    else -> null
                }
        }

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

            is AbilityActionState.Passive.MirrorImage ->
                if (rerollSelection == AbilityAction.Passive.MirrorImage.RerollSelection.None) {
                    null
                } else {
                    ONE_DIE
                }

            is AbilityActionState.Passive.HolyShield.Prayer -> TWO_DICE

            is AbilityActionState.Passive.HolyShield.Wrath -> ONE_DIE
        }
