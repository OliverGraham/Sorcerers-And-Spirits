package com.linklabs.sorcerersandspirits.data.config

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

/**
 * Pins the version-controlled bundled JSON (the in-app RemoteConfig default + Firebase-published
 * value) to the values the rest of the app uses synchronously, so the two can never silently drift.
 * This is what lets the `CharacterClass` factory stat literals stay as the provider's last-resort
 * fallback without becoming a second, divergent source of truth (SS-059).
 */
class BundledRemoteConfigDefaultsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `bundled character_class_stats JSON matches the in-app factory defaults`() {
        val parsed =
            json.decodeFromString<Map<String, CharacterClassStatsEntity>>(
                string = bundledClassStatsJson()
            )

        assertEquals(expected = defaultCharacterClassStats(), actual = parsed)
    }

    @Test
    fun `bundled random_traveler_names JSON contains the offline fallback name`() {
        val names = json.decodeFromString<List<String>>(string = bundledTravelerNamesJson())

        assertEquals(
            expected = true,
            actual = names.contains(FirebaseRemoteConfigDataSource.FALLBACK_TRAVELER_NAME),
        )
    }

    private fun bundledClassStatsJson(): String =
        readBundledResource(fileName = "character_class_stats.json")

    private fun bundledTravelerNamesJson(): String =
        readBundledResource(fileName = "random_traveler_names.json")

    private fun readBundledResource(fileName: String): String {
        val relativePath = "$BUNDLED_RESOURCE_DIR/$fileName"
        val file =
            listOf(relativePath, "composeApp/$relativePath").map(::File).firstOrNull { it.exists() }
                ?: error(
                    "Could not locate bundled $fileName from ${File(".").absolutePath}; " +
                        "expected at $relativePath"
                )
        return file.readText()
    }

    private companion object {
        const val BUNDLED_RESOURCE_DIR = "src/commonMain/composeResources/files/remote_config"
    }
}
