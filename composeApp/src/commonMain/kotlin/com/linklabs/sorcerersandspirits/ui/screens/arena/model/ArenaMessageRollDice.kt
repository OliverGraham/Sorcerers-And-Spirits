package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

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
        is ArenaMessageState.MirrorImage.TemptFate -> listOf(rawReroll)
        else -> null
    }

/**
 * The most recent roll across all players: its die faces, and the id of the message that carried it
 * so a fresh roll can be told apart from the previous one even when it lands on the same faces.
 */
@Immutable
internal data class LatestRoll(val dice: List<Int> = emptyList(), val messageId: String? = null)

/** The most recent roll across all players, or an empty [LatestRoll] if nothing has been rolled. */
internal fun List<ArenaMessageState>.latestRoll(): LatestRoll {
    for (message in asReversed()) {
        val dice = message.rolledDice()
        if (dice != null) return LatestRoll(dice = dice, messageId = message.id)
    }
    return LatestRoll()
}
