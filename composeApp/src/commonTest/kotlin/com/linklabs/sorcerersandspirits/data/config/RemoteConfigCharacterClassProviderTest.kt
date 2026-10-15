package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.TestCharacterClasses
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigCharacterClassProviderTest {

    @Test
    fun `builds a class from remote stats with the type's wired abilities`() {
        val tunedRogue =
            CharacterClassStatsEntity(hitPoints = 9, maxHitPoints = 9, armorClass = 2, speed = 3)
        val provider =
            RemoteConfigCharacterClassProvider(
                remoteConfigDataSource =
                FakeRemoteConfigDataSource(
                    stats = mapOf(CharacterClassType.Rogue to tunedRogue),
                ),
            )

        val rogue = provider.getAllClasses().single { it.type == CharacterClassType.Rogue }
        val wiredRogue = TestCharacterClasses.rogue()

        assertEquals(expected = 9, actual = rogue.hitPoints)
        assertEquals(expected = 9, actual = rogue.maxHitPoints)
        assertEquals(expected = 2, actual = rogue.armorClass)
        assertEquals(expected = 3, actual = rogue.speed)
        assertEquals(expected = wiredRogue.offensiveAbility, actual = rogue.offensiveAbility)
        assertEquals(expected = wiredRogue.defensiveAbility, actual = rogue.defensiveAbility)
    }

    @Test
    fun `omits classes that have no remote stats`() {
        val provider =
            RemoteConfigCharacterClassProvider(
                remoteConfigDataSource = FakeRemoteConfigDataSource(stats = emptyMap()),
            )

        assertEquals(expected = emptyList<CharacterClass>(), actual = provider.getAllClasses())
    }

    @Test
    fun `builds every playable class when all stats are present`() {
        val stats =
            TestCharacterClasses.all().associate { characterClass ->
                characterClass.type to
                    CharacterClassStatsEntity(
                        hitPoints = characterClass.hitPoints,
                        maxHitPoints = characterClass.maxHitPoints,
                        armorClass = characterClass.armorClass,
                        speed = characterClass.speed,
                    )
            }
        val provider =
            RemoteConfigCharacterClassProvider(
                remoteConfigDataSource = FakeRemoteConfigDataSource(stats = stats),
            )

        assertEquals(expected = TestCharacterClasses.all(), actual = provider.getAllClasses())
    }

    private class FakeRemoteConfigDataSource(
        private val stats: Map<CharacterClassType, CharacterClassStatsEntity>,
    ) : RemoteConfigDataSource {
        override suspend fun initialize() = Unit

        override fun characterClassStats() = stats

        override fun abilityConfig() = AbilityConfigEntity.fromDomain(config = AbilityConfig.DEFAULT)

        override fun characterClassCopy() = emptyMap<String, AbilityCopyEntity>()

        override fun randomTravelerNames() = emptyList<String>()

        override fun credits() = CreditsEntity.EMPTY

        override fun minimumSupportedVersionCode() = RemoteConfigDataSource.UNGATED_VERSION

        override fun latestVersionCode() = RemoteConfigDataSource.UNGATED_VERSION

        override fun bugReportEndpointUrl() = ""

        override fun bugReportToken() = ""
    }
}
