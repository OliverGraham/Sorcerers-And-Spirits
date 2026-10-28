package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigCharacterClassCopyProviderTest {

    @Test
    fun `maps each entity's name long and short copy through to the domain`() {
        val holyShield =
            AbilityCopyEntity(name = "Holy Shield", long = "Long text.", short = "Short text.")
        val provider =
            RemoteConfigCharacterClassCopyProvider(
                remoteConfigDataSource =
                FakeRemoteConfigDataSource(
                    copy = mapOf(ClassCopyKeys.HOLY_SHIELD to holyShield),
                ),
            )

        val resolved = provider.getCharacterClassCopy()[ClassCopyKeys.HOLY_SHIELD]

        assertEquals(expected = "Holy Shield", actual = resolved.name)
        assertEquals(expected = "Long text.", actual = resolved.long)
        assertEquals(expected = "Short text.", actual = resolved.short)
    }

    @Test
    fun `resolves an unknown key to empty copy rather than throwing`() {
        val provider =
            RemoteConfigCharacterClassCopyProvider(
                remoteConfigDataSource = FakeRemoteConfigDataSource(copy = emptyMap()),
            )

        assertEquals(
            expected = "",
            actual = provider.getCharacterClassCopy()[ClassCopyKeys.RIPOSTE].name,
        )
    }

    private class FakeRemoteConfigDataSource(private val copy: Map<String, AbilityCopyEntity>) : RemoteConfigDataSource {
        override suspend fun initialize() = Unit

        override fun characterClassStats() = emptyMap<CharacterClassType, CharacterClassStatsEntity>()

        override fun abilityConfig() = AbilityConfigEntity.fromDomain(config = AbilityConfig.DEFAULT)

        override fun characterClassCopy() = copy

        override fun randomTravelerNames() = emptyList<String>()

        override fun credits() = CreditsEntity.EMPTY

        override fun minimumSupportedVersionCode() = RemoteConfigDataSource.UNGATED_VERSION

        override fun latestVersionCode() = RemoteConfigDataSource.UNGATED_VERSION

        override fun bugReportEndpointUrl() = ""

        override fun bugReportToken() = ""

        override fun appleRevokeEndpointUrl() = ""

        override fun appleRevokeToken() = ""
    }
}
