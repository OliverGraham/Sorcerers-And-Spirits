package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharacterClassEntityTest {

    private fun createCharacterClass(hasActedThisRound: Boolean) = CharacterClass.create(
        type = CharacterClassType.Rogue,
        hitPoints = 10,
        maxHitPoints = 10,
        armorClass = 10,
        speed = 5,
    )
        .copy(hasActedThisRound = hasActedThisRound)

    @Test
    fun `hasActedThisRound survives the entity round-trip`() {
        val roundTripped = createCharacterClass(hasActedThisRound = true).toEntity().toDomain()

        assertTrue(roundTripped.hasActedThisRound)
    }

    @Test
    fun `hasActedThisRound false survives the entity round-trip`() {
        val roundTripped = createCharacterClass(hasActedThisRound = false).toEntity().toDomain()

        assertFalse(roundTripped.hasActedThisRound)
    }

    @Test
    fun `resetForNewMatch clears hasActedThisRound`() {
        val reset = createCharacterClass(hasActedThisRound = true).toEntity().resetForNewMatch()

        assertFalse(reset.hasActedThisRound)
    }
}
