package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceRollTest {

    @Test
    fun `committing a two-dice roll settles both dice on their faces`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState = DiceRollState.TwoDice(firstDie = 5, secondDie = 3, doublesBonus = null)
        )

        assertFalse(actual = holder.isRolling)
        assertEquals(expected = 5, actual = holder.firstDieState.value)
        assertEquals(expected = 3, actual = holder.secondDieState.value)
        assertTrue(actual = holder.firstDieState.isActive)
        assertTrue(actual = holder.secondDieState.isActive)
    }

    @Test
    fun `committing a one-die roll activates only the rolled die`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    doublesBonus = null,
                )
        )

        assertFalse(actual = holder.isRolling)
        assertEquals(expected = 4, actual = holder.firstDieState.value)
        assertTrue(actual = holder.firstDieState.isActive)
        assertFalse(actual = holder.secondDieState.isActive)
    }

    @Test
    fun `a turn that resolves without a roll settles instead of stalling`() {
        val holder = releasedRoll()

        holder.settleIfResolvedWithoutResult()

        assertFalse(actual = holder.isRolling)
    }

    /** A roll committed from the resting state, now tumbling while it awaits its result. */
    private fun releasedRoll(): DiceStateHolder {
        val holder = DiceStateHolder()
        holder.startHold(rollingPositions = setOf(DiePosition.First, DiePosition.Second))
        holder.release(isDoubleTap = true) {}
        assertTrue(actual = holder.isRolling)
        return holder
    }
}
