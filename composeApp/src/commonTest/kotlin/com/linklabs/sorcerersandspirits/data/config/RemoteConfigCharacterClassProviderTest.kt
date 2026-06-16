package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigCharacterClassProviderTest {

    @Test
    fun `overlays remote stats onto a class while keeping its abilities`() {
        val tunedRogue =
            CharacterClassStatsEntity(hitPoints = 9, maxHitPoints = 9, armorClass = 2, speed = 3)
        val provider =
            RemoteConfigCharacterClassProvider(
                remoteConfigDataSource =
                    FakeRemoteConfigDataSource(
                        stats = mapOf(CharacterClassType.Rogue to tunedRogue)
                    )
            )

        val rogue = provider.getAllClasses().single { it.type == CharacterClassType.Rogue }
        val factoryRogue = CharacterClass.getRogue()

        assertEquals(expected = 9, actual = rogue.hitPoints)
        assertEquals(expected = 9, actual = rogue.maxHitPoints)
        assertEquals(expected = 2, actual = rogue.armorClass)
        assertEquals(expected = 3, actual = rogue.speed)
        assertEquals(expected = factoryRogue.offensiveAbility, actual = rogue.offensiveAbility)
        assertEquals(expected = factoryRogue.defensiveAbility, actual = rogue.defensiveAbility)
    }

    @Test
    fun `falls back to factory stats for classes without remote overrides`() {
        val provider =
            RemoteConfigCharacterClassProvider(
                remoteConfigDataSource = FakeRemoteConfigDataSource(stats = emptyMap())
            )

        assertEquals(expected = CharacterClass.getAllClasses(), actual = provider.getAllClasses())
    }

    private class FakeRemoteConfigDataSource(
        private val stats: Map<CharacterClassType, CharacterClassStatsEntity>
    ) : RemoteConfigDataSource {
        override suspend fun initialize() = Unit

        override fun characterClassStats() = stats
    }
}
