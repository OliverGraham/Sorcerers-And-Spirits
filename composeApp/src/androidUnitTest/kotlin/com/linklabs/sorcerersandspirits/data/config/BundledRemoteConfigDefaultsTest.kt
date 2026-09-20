package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the version-controlled bundled JSON (the in-app RemoteConfig default + Firebase-published
 * value), which is the single source of truth for class stats (SS-111). The stats payload must
 * parse and cover every playable class, since production has no hardcoded stat fallback to fall
 * back on.
 */
class BundledRemoteConfigDefaultsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `bundled character_class_stats JSON covers every playable class`() {
        val parsed =
            json.decodeFromString<Map<String, CharacterClassStatsEntity>>(
                string = bundledClassStatsJson(),
            )

        val expectedTypeNames = CharacterClass.playableTypes().map { it.name }.toSet()

        assertEquals(expected = expectedTypeNames, actual = parsed.keys)
    }

    @Test
    fun `bundled ability_config JSON matches the shipped AbilityConfig default`() {
        val parsed =
            json
                .decodeFromString<AbilityConfigEntity>(string = bundledAbilityConfigJson())
                .toDomain()

        assertEquals(expected = AbilityConfig.DEFAULT, actual = parsed)
    }

    @Test
    fun `bundled character_class_copy JSON covers every copy key`() {
        val parsed =
            json.decodeFromString<Map<String, AbilityCopyEntity>>(string = bundledClassCopyJson())

        assertEquals(expected = ClassCopyKeys.ALL, actual = parsed.keys)
    }

    @Test
    fun `bundled random_traveler_names JSON contains the offline fallback name`() {
        val names = json.decodeFromString<List<String>>(string = bundledTravelerNamesJson())

        assertEquals(
            expected = true,
            actual = names.contains(GithubJsonRemoteConfigDataSource.FALLBACK_TRAVELER_NAME),
        )
    }

    private fun bundledClassStatsJson(): String = readBundledResource(fileName = "character_class_stats.json")

    private fun bundledAbilityConfigJson(): String = readBundledResource(fileName = "ability_config.json")

    private fun bundledClassCopyJson(): String = readBundledResource(fileName = "character_class_copy.json")

    private fun bundledTravelerNamesJson(): String = readBundledResource(fileName = "random_traveler_names.json")

    private fun readBundledResource(fileName: String): String {
        val relativePath = "$BUNDLED_RESOURCE_DIR/$fileName"
        val file =
            listOf(relativePath, "composeApp/$relativePath").map(::File).firstOrNull { it.exists() }
                ?: error(
                    "Could not locate bundled $fileName from ${File(".").absolutePath}; " +
                        "expected at $relativePath",
                )
        return file.readText()
    }

    private companion object {
        const val BUNDLED_RESOURCE_DIR = "src/commonMain/composeResources/files/remote_config"
    }
}
