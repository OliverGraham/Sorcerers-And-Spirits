package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class CharacterClassStatsEntityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `default stats cover every playable class with matching values`() {
        val defaults = defaultCharacterClassStats()

        val expected =
            CharacterClass.getAllClasses().associate { characterClass ->
                characterClass.type.name to characterClass.toStatsEntity()
            }

        assertEquals(expected = expected, actual = defaults)
    }

    @Test
    fun `default stats payload round-trips through the RemoteConfig JSON schema`() {
        val defaults = defaultCharacterClassStats()

        val payload = json.encodeToString(defaults)
        val parsed = json.decodeFromString<Map<String, CharacterClassStatsEntity>>(payload)

        assertEquals(expected = defaults, actual = parsed)
    }
}
