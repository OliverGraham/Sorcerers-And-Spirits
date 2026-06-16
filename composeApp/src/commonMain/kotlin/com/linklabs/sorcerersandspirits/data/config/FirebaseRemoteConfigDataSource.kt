package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.CHARACTER_CLASS_STATS
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.json.Json

class FirebaseRemoteConfigDataSource(private val remoteConfig: FirebaseRemoteConfig) :
    RemoteConfigDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private val defaultStats: Map<String, CharacterClassStatsEntity> = defaultCharacterClassStats()

    override suspend fun initialize() {
        remoteConfig.settings { minimumFetchInterval = MINIMUM_FETCH_INTERVAL }
        remoteConfig.setDefaults(CHARACTER_CLASS_STATS to json.encodeToString(value = defaultStats))
        remoteConfig.fetchAndActivate()
    }

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

    companion object {
        val MINIMUM_FETCH_INTERVAL: Duration = 1.hours
    }
}
