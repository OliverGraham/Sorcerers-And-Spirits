package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.ABILITY_CONFIG
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.CHARACTER_CLASS_COPY
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.CHARACTER_CLASS_STATS
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.CREDITS
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.RANDOM_TRAVELER_NAMES
import com.linklabs.sorcerersandspirits.data.config.RemoteConfigKeys.VERSION_GATE
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.Json
import sorcerersandspirits.composeapp.generated.resources.Res
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * TODO: Use something like Moshi that can generate data classes from Json, so none of this
 *   [ClassCopyKeys] bullshit would have to be used. Or, why did Claude use a map in the first
 *   place? Why not a data class?
 */
class FirebaseRemoteConfigDataSource(private val remoteConfig: FirebaseRemoteConfig) : RemoteConfigDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    // Empty until initialize() loads the bundled JSON at app start (before any class is built). The
    // bundled JSON is the single source of truth for default stats, so there is no code-level seed.
    private var defaultStats: Map<String, CharacterClassStatsEntity> = emptyMap()

    // Seeded from the shipped balance so abilityConfig() always resolves, then overwritten by the
    // bundled JSON (the published default) at init.
    private var defaultAbilityConfig: AbilityConfigEntity =
        AbilityConfigEntity.fromDomain(config = AbilityConfig.DEFAULT)

    // Empty until initialize() loads the bundled JSON; the bundled copy is the single source of
    // truth for default copy, so there is no code-level seed.
    private var defaultClassCopy: Map<String, AbilityCopyEntity> = emptyMap()

    private var defaultNames: List<String> = listOf(FALLBACK_TRAVELER_NAME)

    private var defaultCredits: CreditsEntity = CreditsEntity.EMPTY

    private var defaultVersionGate: VersionGateEntity = VersionGateEntity.UNGATED

    override suspend fun initialize() {
        defaultStats =
            loadBundledDefault(path = BUNDLED_CLASS_STATS_PATH, fallback = defaultStats).ifEmpty {
                defaultStats
            }
        defaultAbilityConfig =
            loadBundledDefault(path = BUNDLED_ABILITY_CONFIG_PATH, fallback = defaultAbilityConfig)
        defaultClassCopy =
            loadBundledDefault(path = BUNDLED_CLASS_COPY_PATH, fallback = defaultClassCopy)
                .ifEmpty { defaultClassCopy }
        defaultNames =
            loadBundledDefault(path = BUNDLED_TRAVELER_NAMES_PATH, fallback = defaultNames)
                .ifEmpty { defaultNames }
        defaultCredits = loadBundledDefault(path = BUNDLED_CREDITS_PATH, fallback = defaultCredits)
        defaultVersionGate =
            loadBundledDefault(path = BUNDLED_VERSION_GATE_PATH, fallback = defaultVersionGate)
        remoteConfig.settings { minimumFetchInterval = MINIMUM_FETCH_INTERVAL }
        remoteConfig.setDefaults(
            CHARACTER_CLASS_STATS to json.encodeToString(value = defaultStats),
            ABILITY_CONFIG to json.encodeToString(value = defaultAbilityConfig),
            CHARACTER_CLASS_COPY to json.encodeToString(value = defaultClassCopy),
            RANDOM_TRAVELER_NAMES to json.encodeToString(value = defaultNames),
            CREDITS to json.encodeToString(value = defaultCredits),
            VERSION_GATE to json.encodeToString(value = defaultVersionGate),
        )
        remoteConfig.fetchAndActivate()
    }

    private suspend inline fun <reified T> loadBundledDefault(path: String, fallback: T): T = runCatching {
        val bytes = Res.readBytes(path = path)
        json.decodeFromString<T>(string = bytes.decodeToString())
    }
        .getOrDefault(defaultValue = fallback)

    override fun characterClassStats(): Map<CharacterClassType, CharacterClassStatsEntity> = runCatching {
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

    override fun abilityConfig(): AbilityConfigEntity = runCatching {
        val raw = remoteConfig.getValue(key = ABILITY_CONFIG).asString()
        json.decodeFromString<AbilityConfigEntity>(string = raw)
    }
        .getOrDefault(defaultValue = defaultAbilityConfig)

    override fun characterClassCopy(): Map<String, AbilityCopyEntity> = runCatching {
        val raw = remoteConfig.getValue(key = CHARACTER_CLASS_COPY).asString()
        json.decodeFromString<Map<String, AbilityCopyEntity>>(string = raw)
    }
        .getOrDefault(defaultValue = defaultClassCopy)
        .ifEmpty { defaultClassCopy }

    override fun randomTravelerNames(): List<String> = runCatching {
        val raw = remoteConfig.getValue(key = RANDOM_TRAVELER_NAMES).asString()
        json.decodeFromString<List<String>>(string = raw)
    }
        .getOrDefault(defaultValue = defaultNames)
        .ifEmpty { defaultNames }

    override fun credits(): CreditsEntity = runCatching {
        val raw = remoteConfig.getValue(key = CREDITS).asString()
        json.decodeFromString<CreditsEntity>(string = raw)
    }
        .getOrDefault(defaultValue = defaultCredits)

    override fun minimumSupportedVersionCode(): Long = versionGate().minimumSupportedVersionCode

    override fun latestVersionCode(): Long = versionGate().latestVersionCode

    private fun versionGate(): VersionGateEntity = runCatching {
        val raw = remoteConfig.getValue(key = VERSION_GATE).asString()
        json.decodeFromString<VersionGateEntity>(string = raw)
    }
        .getOrDefault(defaultValue = defaultVersionGate)

    companion object {
        val MINIMUM_FETCH_INTERVAL: Duration = 1.hours
        const val BUNDLED_CLASS_STATS_PATH = "files/remote_config/character_class_stats.json"
        const val BUNDLED_ABILITY_CONFIG_PATH = "files/remote_config/ability_config.json"
        const val BUNDLED_CLASS_COPY_PATH = "files/remote_config/character_class_copy.json"
        const val BUNDLED_TRAVELER_NAMES_PATH = "files/remote_config/random_traveler_names.json"
        const val BUNDLED_CREDITS_PATH = "files/remote_config/credits.json"
        const val BUNDLED_VERSION_GATE_PATH = "files/remote_config/version_gate.json"
        const val FALLBACK_TRAVELER_NAME = "a mysterious traveler"
    }
}
