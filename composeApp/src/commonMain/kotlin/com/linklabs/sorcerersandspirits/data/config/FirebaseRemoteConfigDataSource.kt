package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.CHARACTER_CLASS_STATS
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.RANDOM_TRAVELER_NAMES
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.json.Json
import sorcerersandspirits.composeapp.generated.resources.Res

class FirebaseRemoteConfigDataSource(private val remoteConfig: FirebaseRemoteConfig) :
    RemoteConfigDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    // Loaded from the bundled JSON in initialize(); seeded so a getter racing ahead of that load
    // never sees an empty payload. BundledRemoteConfigDefaultsTest pins the stats JSON to the seed.
    private var defaultStats: Map<String, CharacterClassStatsEntity> = defaultCharacterClassStats()

    private var defaultNames: List<String> = listOf(FALLBACK_TRAVELER_NAME)

    override suspend fun initialize() {
        defaultStats =
            loadBundledDefault(path = BUNDLED_CLASS_STATS_PATH, fallback = defaultStats).ifEmpty {
                defaultStats
            }
        defaultNames =
            loadBundledDefault(path = BUNDLED_TRAVELER_NAMES_PATH, fallback = defaultNames)
                .ifEmpty { defaultNames }
        remoteConfig.settings { minimumFetchInterval = MINIMUM_FETCH_INTERVAL }
        remoteConfig.setDefaults(
            CHARACTER_CLASS_STATS to json.encodeToString(value = defaultStats),
            RANDOM_TRAVELER_NAMES to json.encodeToString(value = defaultNames),
        )
        remoteConfig.fetchAndActivate()
    }

    private suspend inline fun <reified T> loadBundledDefault(path: String, fallback: T): T =
        runCatching {
                val bytes = Res.readBytes(path = path)
                json.decodeFromString<T>(string = bytes.decodeToString())
            }
            .getOrDefault(defaultValue = fallback)

    override fun characterClassStats(): Map<CharacterClassType, CharacterClassStatsEntity> =
        runCatching {
                val raw = remoteConfig.getValue(key = CHARACTER_CLASS_STATS).asString()
                json.decodeFromString<Map<String, CharacterClassStatsEntity>>(string = raw)
            }
            .getOrDefault(defaultValue = defaultStats)
            .mapNotNull { (name, stats) ->
                runCatching { CharacterClassType.valueOf(value = name) }
                    .getOrNull()
                    ?.let { it to stats }
            }
            .toMap()

    override fun randomTravelerNames(): List<String> =
        runCatching {
                val raw = remoteConfig.getValue(key = RANDOM_TRAVELER_NAMES).asString()
                json.decodeFromString<List<String>>(string = raw)
            }
            .getOrDefault(defaultValue = defaultNames)
            .ifEmpty { defaultNames }

    companion object {
        val MINIMUM_FETCH_INTERVAL: Duration = 1.hours
        const val BUNDLED_CLASS_STATS_PATH = "files/remote_config/character_class_stats.json"
        const val BUNDLED_TRAVELER_NAMES_PATH = "files/remote_config/random_traveler_names.json"
        const val FALLBACK_TRAVELER_NAME = "a mysterious traveler"
    }
}
