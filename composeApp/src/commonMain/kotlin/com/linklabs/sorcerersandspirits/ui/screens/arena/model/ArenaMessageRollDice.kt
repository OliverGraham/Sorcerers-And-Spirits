package com.linklabs.sorcerersandspirits.ui.screens.arena.model

/**
 * The raw die faces this message reported, or null for messages that carry no roll. Single-die
 * rolls return one face; two-die rolls return both. Used to drive the shake-to-roll dice so they
 * settle on the value the Domain actually rolled (SS-077).
 */
internal fun ArenaMessageState.rolledDice(): List<Int>? =
    when (this) {
        is ArenaMessageState.NormalRoll -> listOf(firstDie, secondDie)
        is ArenaMessageState.PrepareAttack -> listOf(firstDie, secondDie)
        is ArenaMessageState.Riposte.EnGarde -> listOf(firstDie, secondDie)
        is ArenaMessageState.HolyShield.Prayer -> listOf(firstDie, secondDie)
        is ArenaMessageState.HolyShield.Wrath -> listOf(singleDie)
        is ArenaMessageState.Teleport.Roll -> listOf(singleDie)
        is ArenaMessageState.LuckyCoin.Flip -> listOf(singleDie)
        is ArenaMessageState.LuckyCoin.HighRollerDamage -> listOf(singleDie)
        is ArenaMessageState.SummonMinion -> listOf(singleDie)
        is ArenaMessageState.Persuade.StrumLute -> listOf(diceRoll)
        is ArenaMessageState.Encore.Roll -> listOf(encoreDie)
        is ArenaMessageState.AllIn -> listOf(singleDie)
        is ArenaMessageState.MirrorImage.TemptFate -> listOf(finalRoll)
        else -> null
    }

/**
 * The die faces of the most recent roll across all players, or empty if nothing has been rolled.
 */
internal fun List<ArenaMessageState>.latestRolledDice(): List<Int> =
    asReversed().firstNotNullOfOrNull { it.rolledDice() } ?: emptyList()
