package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice

import com.linklabs.sorcerersandspirits.domain.arena.model.DieColor
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiceRollTest {

    @Test
    fun `committing a two-dice roll settles both dice on their faces`() {
        val holder = releasedRoll()

        holder.commitRoll(diceRollState = DiceRollState.TwoDice(firstDie = 5, secondDie = 3))

        assertFalse(actual = holder.isRolling)
        assertEquals(expected = 5, actual = holder.firstDieState.value)
        assertEquals(expected = 3, actual = holder.secondDieState.value)
        assertTrue(actual = holder.firstDieState.isEnabled)
        assertTrue(actual = holder.secondDieState.isEnabled)
    }

    @Test
    fun `committing a solo one-die roll greys the untouched die`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    dieColor = DieColor.Bard,
                    isSiblingDisabled = true,
                )
        )

        assertFalse(actual = holder.isRolling)
        assertEquals(expected = 4, actual = holder.firstDieState.value)
        assertTrue(actual = holder.firstDieState.isEnabled)
        assertFalse(actual = holder.secondDieState.isEnabled)
    }

    @Test
    fun `a roll-off roll colors only its own die and leaves the other untouched`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    dieColor = DieColor.Rogue,
                    isSiblingDisabled = false,
                )
        )

        assertEquals(expected = DieColor.Rogue.toColor(), actual = holder.firstDieState.tint)
        assertTrue(actual = holder.firstDieState.isEnabled)
        assertNull(actual = holder.secondDieState.tint)
        assertTrue(actual = holder.secondDieState.isEnabled)
    }

    @Test
    fun `the first combatant's die keeps its color and value once the second combatant rolls`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    dieColor = DieColor.Rogue,
                    isSiblingDisabled = false,
                )
        )

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 2,
                    diePosition = DiePosition.Second,
                    dieColor = DieColor.Mage,
                    isSiblingDisabled = false,
                )
        )

        assertEquals(expected = DieColor.Rogue.toColor(), actual = holder.firstDieState.tint)
        assertEquals(expected = 4, actual = holder.firstDieState.value)
        assertTrue(actual = holder.firstDieState.isEnabled)
        assertEquals(expected = DieColor.Mage.toColor(), actual = holder.secondDieState.tint)
        assertEquals(expected = 2, actual = holder.secondDieState.value)
    }

    @Test
    fun `each die keeps its own settle generation across a roll-off`() {
        val holder = releasedRoll()

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 4,
                    diePosition = DiePosition.First,
                    dieColor = DieColor.Rogue,
                    isSiblingDisabled = false,
                )
        )

        holder.commitRoll(
            diceRollState =
                DiceRollState.OneDie(
                    value = 6,
                    diePosition = DiePosition.Second,
                    dieColor = DieColor.Mage,
                    isSiblingDisabled = false,
                )
        )

        assertEquals(expected = 1, actual = holder.firstDieState.settleGeneration)
        assertEquals(expected = 1, actual = holder.secondDieState.settleGeneration)
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
