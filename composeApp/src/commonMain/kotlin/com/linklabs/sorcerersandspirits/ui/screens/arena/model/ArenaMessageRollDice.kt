package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

private const val FIRST_DIE_SLOT = 0
private const val SECOND_DIE_SLOT = 1

/**
 * What a roll-bearing message did to the shared dice, like two real dice sitting on a table that
 * everyone watches.
 */
@Immutable
internal sealed interface RollResult {
    @Immutable data class Faces(val slotFaces: Map<Int, Int>) : RollResult

    @Immutable data class Reroll(val originalFace: Int, val newFace: Int) : RollResult
}

internal fun ArenaMessageState.rolledDice(): RollResult? =
    when (this) {
        is ArenaMessageState.NormalRoll -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessageState.PrepareAttack -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessageState.SpellPotency -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessageState.Riposte.EnGarde -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessageState.HolyShield.Prayer -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessageState.HolyShield.Wrath -> oneDie(face = singleDie)

        is ArenaMessageState.Teleport.Roll -> oneDie(face = singleDie)

        is ArenaMessageState.LuckyCoin.Flip -> oneDie(face = singleDie)

        is ArenaMessageState.LuckyCoin.HighRollerDamage -> oneDie(face = singleDie)

        is ArenaMessageState.SummonMinion -> oneDie(face = singleDie)

        is ArenaMessageState.Persuade.StrumLute -> oneDie(face = diceRoll)

        is ArenaMessageState.Encore.Roll -> oneDie(face = encoreDie)

        is ArenaMessageState.AllIn -> oneDie(face = singleDie)

        is ArenaMessageState.MirrorImage.TemptFate ->
            RollResult.Reroll(originalFace = originalRoll, newFace = rawReroll)

        is ArenaMessageState.MirrorImage.Trigger ->
            if (firstDie != null && secondDie != null) {
                twoDice(first = firstDie, second = secondDie)
            } else {
                null
            }

        else -> null
    }

private fun oneDie(face: Int): RollResult.Faces =
    RollResult.Faces(slotFaces = mapOf(FIRST_DIE_SLOT to face))

private fun twoDice(first: Int, second: Int): RollResult.Faces =
    RollResult.Faces(slotFaces = mapOf(FIRST_DIE_SLOT to first, SECOND_DIE_SLOT to second))

/**
 * The most recent roll across all players: what it did to the dice, and the id of the message that
 * carried it so a fresh roll can be told apart from the previous one even when it lands on the same
 * faces.
 */
@Immutable
internal data class LatestRoll(
    val result: RollResult? = null,
    val messageId: String? = null,
    val latestMessageId: String? = null,
)

internal fun List<ArenaMessageState>.latestRoll(): LatestRoll {
    val latestMessageId = lastOrNull()?.id
    for (message in asReversed()) {
        val result = message.rolledDice()
        if (result != null) {
            return LatestRoll(
                result = result,
                messageId = message.id,
                latestMessageId = latestMessageId,
            )
        }
    }
    return LatestRoll(latestMessageId = latestMessageId)
}
