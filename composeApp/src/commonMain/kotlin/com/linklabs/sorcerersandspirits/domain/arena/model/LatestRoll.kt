package com.linklabs.sorcerersandspirits.domain.arena.model

private const val FIRST_DIE_SLOT = 0
private const val SECOND_DIE_SLOT = 1

// TODO: DELETE

/**
 * The single roll a combat action produced, captured where it happened so no layer has to search
 * the message log to rebuild it. [messageId] identifies the roll so a fresh one is told apart from
 * the previous, and [latestMessageId] is the newest message of any kind so a turn that produced no
 * roll still settles a pending roll on this device.
 */
data class LatestRoll(
    val result: RolledDice? = null,
    val messageId: String? = null,
    val latestMessageId: String? = null,
)

sealed interface RolledDice {
    data class Faces(val slotFaces: Map<Int, Int>) : RolledDice

    data class Reroll(val originalFace: Int, val newFace: Int) : RolledDice
}

fun ArenaMessage.rolledDice(): RolledDice? =
    when (this) {
        is ArenaMessage.NormalRoll -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessage.PrepareAttack -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessage.SpellPotency -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessage.Riposte.EnGarde -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessage.HolyShield.Prayer -> twoDice(first = firstDie, second = secondDie)

        is ArenaMessage.HolyShield.Wrath -> oneDie(face = singleDie)

        is ArenaMessage.Teleport.Roll -> oneDie(face = singleDie)

        is ArenaMessage.LuckyCoin.Flip -> oneDie(face = singleDie)

        is ArenaMessage.LuckyCoin.HighRollerDamage -> oneDie(face = singleDie)

        is ArenaMessage.SummonMinion -> oneDie(face = singleDie)

        is ArenaMessage.Persuade.StrumLute -> oneDie(face = diceRoll)

        is ArenaMessage.Encore.Roll -> oneDie(face = encoreDie)

        is ArenaMessage.AllIn -> oneDie(face = singleDie)

        is ArenaMessage.MirrorImage.TemptFate ->
            RolledDice.Reroll(
                originalFace = rerolledMirrorImageDie.originalRoll,
                newFace = rerolledMirrorImageDie.reroll ?: rerolledMirrorImageDie.originalRoll,
            )

        is ArenaMessage.MirrorImage.Trigger ->
            if (firstDie != null && secondDie != null) {
                twoDice(first = firstDie, second = secondDie)
            } else {
                null
            }

        else -> null
    }

private fun oneDie(face: Int): RolledDice.Faces =
    RolledDice.Faces(slotFaces = mapOf(pair = FIRST_DIE_SLOT to face))

private fun twoDice(first: Int, second: Int): RolledDice.Faces =
    RolledDice.Faces(slotFaces = mapOf(FIRST_DIE_SLOT to first, SECOND_DIE_SLOT to second))
