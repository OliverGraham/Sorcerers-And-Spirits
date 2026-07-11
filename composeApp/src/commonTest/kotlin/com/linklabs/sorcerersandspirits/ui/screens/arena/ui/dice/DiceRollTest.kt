package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.RollResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceRollTest {

    @Test
    fun `own roll result settles the dice on its faces`() {
        val roll = releasedRoll()

        roll.receiveRoll(rollMessageId = "r1", faces = mapOf(0 to 5, 1 to 3))

        assertFalse(actual = roll.isRollPending)
        assertFalse(actual = roll.isRolling)
        assertEquals(expected = listOf(5, 3), actual = roll.dieFaces)
    }

    @Test
    fun `a turn that resolves without a roll settles instead of stalling`() {
        val roll = releasedRoll()

        roll.receiveNonRollMessage(messageId = "m1")

        assertFalse(actual = roll.isRollPending)
        assertFalse(actual = roll.isRolling)
    }

    @Test
    fun `the guard keeps tumbling while the result has not arrived`() {
        val roll = releasedRoll()

        roll.settleIfResolvedWithoutResult()

        assertTrue(actual = roll.isRollPending)
        assertTrue(actual = roll.isRolling)
    }

    /** A roll committed from the resting state, now tumbling while it awaits its result. */
    private fun releasedRoll(): DiceRoll {
        val roll = DiceRoll()
        roll.latestRollId = "r0"
        roll.latestMessageId = "r0"
        roll.startHold(slots = setOf(0, 1))
        roll.release(isDoubleTap = true) {}
        assertTrue(actual = roll.isRolling)
        return roll
    }

    /** A batch that carries this device's roll result, mirroring [rememberDiceRoll]. */
    private fun DiceRoll.receiveRoll(rollMessageId: String, faces: Map<Int, Int>) {
        val previousRollId = latestRollId
        latestRollId = rollMessageId
        latestMessageId = rollMessageId
        val isOwnRoll = isResultOfOwnRoll(previousRollId = previousRollId)
        commitRoll(result = RollResult.Faces(slotFaces = faces), isOwnRoll = isOwnRoll)
        settleIfResolvedWithoutResult()
    }

    /** A batch whose only message rolled no dice, so the roll-bearing id does not advance. */
    private fun DiceRoll.receiveNonRollMessage(messageId: String) {
        latestMessageId = messageId
        settleIfResolvedWithoutResult()
    }
}
