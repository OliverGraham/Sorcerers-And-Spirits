package com.linklabs.sorcerersandspirits.data.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class CharacterClassStatsEntityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `stats payload round-trips through the RemoteConfig JSON schema`() {
        val stats =
            mapOf(
                "Rogue" to
                    CharacterClassStatsEntity(
                        hitPoints = 5,
                        maxHitPoints = 5,
                        armorClass = 6,
                        speed = 8,
                    ),
                "Mage" to
                    CharacterClassStatsEntity(
                        hitPoints = 4,
                        maxHitPoints = 4,
                        armorClass = 4,
                        speed = 2,
                    ),
            )

        val payload = json.encodeToString(stats)
        val parsed = json.decodeFromString<Map<String, CharacterClassStatsEntity>>(payload)

        assertEquals(expected = stats, actual = parsed)
    }
}
