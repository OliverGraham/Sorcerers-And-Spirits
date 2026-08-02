package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArenaMessageGroupingTest {

    @Test
    fun `messages before the first new round land in the setup group`() {
        val messages =
            listOf(connected(id = "a", name = "Alice"), connected(id = "b", name = "Bob"))

        val groups = messages.toMessageGroups()

        val setup = assertIs<ArenaMessageGroupState.Setup>(groups.single())
        assertEquals(expected = listOf("a", "b"), actual = setup.messages.map { it.id })
    }

    @Test
    fun `a new round opens a group and following messages pile into it`() {
        val messages =
            listOf(newRound(id = "r1", roundNumber = 1), doubles(id = "m1"), doubles(id = "m2"))

        val groups = messages.toMessageGroups()

        val round = assertIs<ArenaMessageGroupState.Round>(groups.single())
        assertEquals(expected = 1, actual = round.roundNumber)
        assertEquals(expected = listOf("r1", "m1", "m2"), actual = round.messages.map { it.id })
    }

    @Test
    fun `rounds appear in encounter order each holding its own messages`() {
        val messages =
            listOf(
                newRound(id = "r1", roundNumber = 1),
                doubles(id = "m1"),
                newRound(id = "r2", roundNumber = 2),
                doubles(id = "m2"),
            )

        val groups = messages.toMessageGroups()

        assertEquals(expected = 2, actual = groups.size)
        val first = assertIs<ArenaMessageGroupState.Round>(groups[0])
        val second = assertIs<ArenaMessageGroupState.Round>(groups[1])
        assertEquals(expected = 1, actual = first.roundNumber)
        assertEquals(expected = 2, actual = second.roundNumber)
        assertEquals(expected = listOf("r1", "m1"), actual = first.messages.map { it.id })
        assertEquals(expected = listOf("r2", "m2"), actual = second.messages.map { it.id })
    }

    @Test
    fun `plusMessage appends to the current round without recomputing earlier groups`() {
        val existing =
            listOf(newRound(id = "r1", roundNumber = 1), doubles(id = "m1")).toMessageGroups()

        val afterSameRound = existing.plusMessage(message = doubles(id = "m2"))

        val round = assertIs<ArenaMessageGroupState.Round>(afterSameRound.single())
        assertEquals(expected = listOf("r1", "m1", "m2"), actual = round.messages.map { it.id })

        val afterNextRound =
            afterSameRound.plusMessage(message = newRound(id = "r2", roundNumber = 2))

        assertEquals(expected = 2, actual = afterNextRound.size)
        assertEquals(
            expected = listOf("r1", "m1", "m2"),
            actual = afterNextRound[0].messages.map { it.id },
        )
        val nextRound = assertIs<ArenaMessageGroupState.Round>(afterNextRound[1])
        assertEquals(expected = 2, actual = nextRound.roundNumber)
        assertEquals(expected = listOf("r2"), actual = nextRound.messages.map { it.id })
    }

    private fun newRound(id: String, roundNumber: Int) =
        ArenaMessageState.NewRound(id = id, roundNumber = roundNumber.toString())

    private fun doubles(id: String) = ArenaMessageState.Doubles(id = id, playerName = "Rogue")

    private fun connected(id: String, name: String) =
        ArenaMessageState.ConnectedToArena(id = id, displayName = name)
}
