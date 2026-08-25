package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.data.arena.utilities.determineNextChoosingPlayerId
import com.linklabs.sorcerersandspirits.data.arena.utilities.getFastestPlayer
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArenaMatchEntityExtensionsTest {

    private fun createSelectedPlayer(userId: String, speed: Int) = ArenaPlayerEntity.Selected(
        userId = userId,
        displayName = "Player $userId",
        characterClassEntity =
        CharacterClassEntity(
            type = CharacterClassType.Rogue,
            hitPoints = 10,
            armorClass = 10,
            speed = speed,
            offensiveAbility = OffensiveAbilityEntity.KillingMomentum,
            defensiveAbility = DefensiveAbilityEntity.Dodge(armorBonus = 0),
        ),
    )

    private fun createUnselectedPlayer(userId: String) = ArenaPlayerEntity.Unselected(userId = userId, displayName = "Player $userId")

    @Test
    fun `getFastestPlayerId returns the player with highest speed`() {
        val players =
            mapOf(
                "1" to createSelectedPlayer("1", 5),
                "2" to createSelectedPlayer("2", 10),
                "3" to createSelectedPlayer("3", 7),
            )

        assertEquals("2", players.getFastestPlayer().userId)
    }

    @Test
    fun `getFastestPlayerId ignores unselected players`() {
        val players =
            mapOf(
                "1" to createSelectedPlayer("1", 5),
                "2" to createUnselectedPlayer("2"),
                "3" to createSelectedPlayer("3", 3),
            )

        assertEquals("1", players.getFastestPlayer().userId)
    }

    @Test
    fun `determineNextChoosingPlayerId returns unselected player if available`() {
        val players =
            mapOf("1" to createSelectedPlayer("1", 10), "2" to createUnselectedPlayer("2"))

        assertEquals("2", players.determineNextChoosingPlayerId())
    }

    @Test
    fun `determineNextChoosingPlayerId returns fastest player if all selected`() {
        val players =
            mapOf("1" to createSelectedPlayer("1", 5), "2" to createSelectedPlayer("2", 10))

        assertEquals("2", players.determineNextChoosingPlayerId())
    }

    @Test
    fun `determineNextChoosingPlayerId returns random unselected if multiple available`() {
        val players = mapOf("1" to createUnselectedPlayer("1"), "2" to createUnselectedPlayer("2"))

        val nextId = players.determineNextChoosingPlayerId()
        assertNotNull(nextId)
        assertTrue(nextId in listOf("1", "2"))
    }
}
