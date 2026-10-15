package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.IncomingArenaMessage
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArenaMessageGroupingTest {

    @Test
    fun `messages before the first new round land in the setup group`() {
        val groups =
            emptyGroups().appendNewMessages(
                incomingMessages =
                listOf(connected(id = "a", name = "Alice"), connected(id = "b", name = "Bob")),
            )

        val setup = assertIs<ArenaMessageGroupState.Setup>(groups.single())
        assertEquals(expected = listOf("a", "b"), actual = setup.messages.map { it.id })
    }

    @Test
    fun `a new round opens a group and following messages pile into it`() {
        val groups =
            emptyGroups().appendNewMessages(
                incomingMessages =
                listOf(newRound(id = "r1", roundNumber = 1), doubles(id = "m1"), doubles(id = "m2")),
            )

        val round = assertIs<ArenaMessageGroupState.Round>(groups.single())
        assertEquals(expected = 1, actual = round.roundNumber)
        assertEquals(expected = listOf("r1", "m1", "m2"), actual = round.messages.map { it.id })
    }

    @Test
    fun `rounds appear in encounter order each holding its own messages`() {
        val groups =
            emptyGroups().appendNewMessages(
                incomingMessages =
                listOf(
                    newRound(id = "r1", roundNumber = 1),
                    doubles(id = "m1"),
                    newRound(id = "r2", roundNumber = 2),
                    doubles(id = "m2"),
                ),
            )

        assertEquals(expected = 2, actual = groups.size)
        val first = assertIs<ArenaMessageGroupState.Round>(groups[0])
        val second = assertIs<ArenaMessageGroupState.Round>(groups[1])
        assertEquals(expected = 1, actual = first.roundNumber)
        assertEquals(expected = 2, actual = second.roundNumber)
        assertEquals(expected = listOf("r1", "m1"), actual = first.messages.map { it.id })
        assertEquals(expected = listOf("r2", "m2"), actual = second.messages.map { it.id })
    }

    @Test
    fun `a combat append lands in the current round without recomputing earlier groups`() {
        val existing =
            emptyGroups().appendNewMessages(
                incomingMessages = listOf(newRound(id = "r1", roundNumber = 1), doubles(id = "m1")),
            )

        val afterSameRound =
            existing.addNewMessageDuringCombat(message = doublesState(id = "m2"))

        val round = assertIs<ArenaMessageGroupState.Round>(afterSameRound.single())
        assertEquals(expected = listOf("r1", "m1", "m2"), actual = round.messages.map { it.id })

        val afterNextRound =
            afterSameRound.addNewMessageDuringCombat(
                message = newRoundState(id = "r2", roundNumber = 2),
            )

        assertEquals(expected = 2, actual = afterNextRound.size)
        assertEquals(
            expected = listOf("r1", "m1", "m2"),
            actual = afterNextRound[0].messages.map { it.id },
        )
        val nextRound = assertIs<ArenaMessageGroupState.Round>(afterNextRound[1])
        assertEquals(expected = 2, actual = nextRound.roundNumber)
        assertEquals(expected = listOf("r2"), actual = nextRound.messages.map { it.id })
    }

    private fun emptyGroups(): List<ArenaMessageGroupState> = emptyList()

    private fun newRound(id: String, roundNumber: Int) = IncomingArenaMessage(
        id = id,
        roundNumber = roundNumber,
        message = ArenaMessage.NewRound(roundNumber = roundNumber),
    )

    private fun doubles(id: String) = IncomingArenaMessage(
        id = id,
        roundNumber = 1,
        message = ArenaMessage.Doubles(className = "Rogue", classType = CharacterClassType.Rogue),
    )

    private fun connected(id: String, name: String) = IncomingArenaMessage(
        id = id,
        roundNumber = 0,
        message = ArenaMessage.ConnectedToArena(displayName = name),
    )

    private fun doublesState(id: String) = ArenaMessageState.Doubles(
        id = id,
        className = "Rogue",
        classType = CharacterClassType.Rogue,
    )

    private fun newRoundState(id: String, roundNumber: Int) = ArenaMessageState.NewRound(id = id, roundNumber = roundNumber)
}
