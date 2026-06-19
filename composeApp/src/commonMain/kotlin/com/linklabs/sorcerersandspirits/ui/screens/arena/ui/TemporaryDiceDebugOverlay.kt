// TODO(SS-091): TEMPORARY combat dice debug overlay. Delete this whole file and its single call
//  site in LoadedArenaScreen once combat is stable. It exists only to surface raw dice immediately
//  instead of waiting for the throttled message log.
package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState

private const val DEBUG_HEADER = "DICE DEBUG (SS-091 • temporary)"
private const val MAX_ROLLS_SHOWN = 2

private data class DebugRoll(val label: String, val dice: List<Int>)

/**
 * Renders the dice from the most recent dice-bearing combat messages, newest first, showing each
 * die individually and their combined total. Reads the raw, un-throttled [messages] list so rolls
 * appear the instant the Domain emits them rather than when the message log reveals them.
 */
@Composable
internal fun TemporaryDiceDebugOverlay(
    messages: List<ArenaMessageState>,
    modifier: Modifier = Modifier,
) {
    val recentRolls =
        remember(messages) {
            messages.mapNotNull { it.toDebugRoll() }.takeLast(n = MAX_ROLLS_SHOWN).reversed()
        }

    if (recentRolls.isEmpty()) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(size = 8.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = DEBUG_HEADER,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            recentRolls.forEach { roll -> DebugRollRow(roll = roll) }
        }
    }
}

@Composable
private fun DebugRollRow(roll: DebugRoll, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = roll.label, style = MaterialTheme.typography.bodySmall)
        roll.dice.forEach { die -> DieChip(value = die) }
        if (roll.dice.size > 1) {
            Text(
                text = "= ${roll.dice.sum()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DieChip(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        modifier =
            modifier
                .clip(shape = RoundedCornerShape(size = 4.dp))
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
    )
}

/**
 * Pulls the raw dice out of a message, or null for messages that carry no dice. Single-die rolls
 * return a one-element list; two-die rolls return both faces so the row can show them individually
 * and summed.
 */
private fun ArenaMessageState.toDebugRoll(): DebugRoll? =
    when (this) {
        is ArenaMessageState.NormalRoll ->
            DebugRoll(label = "Normal ${type.name}", dice = listOf(firstDie, secondDie))

        is ArenaMessageState.PrepareAttack ->
            DebugRoll(label = "Prepare Attack", dice = listOf(firstDie, secondDie))

        is ArenaMessageState.Riposte.EnGarde ->
            DebugRoll(label = "Riposte", dice = listOf(firstDie, secondDie))

        is ArenaMessageState.HolyShield.Prayer ->
            DebugRoll(label = "Holy Shield Prayer", dice = listOf(firstDie, secondDie))

        is ArenaMessageState.HolyShield.Wrath ->
            DebugRoll(label = "Holy Shield Wrath", dice = listOf(singleDie))

        is ArenaMessageState.Teleport.Roll ->
            DebugRoll(label = "Teleport ($playerName)", dice = listOf(singleDie))

        is ArenaMessageState.LuckyCoin.Flip ->
            DebugRoll(label = "Lucky Coin Flip", dice = listOf(singleDie))

        is ArenaMessageState.LuckyCoin.HighRollerDamage ->
            DebugRoll(label = "Lucky Coin Damage", dice = listOf(singleDie))

        is ArenaMessageState.SummonMinion ->
            DebugRoll(label = "Summon Minion", dice = listOf(singleDie))

        is ArenaMessageState.Persuade.StrumLute ->
            DebugRoll(label = "Persuade", dice = listOf(diceRoll))

        is ArenaMessageState.Encore.Roll -> DebugRoll(label = "Encore", dice = listOf(encoreDie))

        is ArenaMessageState.AllIn -> DebugRoll(label = "All-In", dice = listOf(singleDie))

        is ArenaMessageState.MirrorImage.TemptFate ->
            DebugRoll(label = "Mirror Image ($originalRoll→$finalRoll)", dice = listOf(finalRoll))

        else -> null
    }
