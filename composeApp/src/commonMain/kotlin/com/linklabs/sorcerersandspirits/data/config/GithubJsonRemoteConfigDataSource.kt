package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import sorcerersandspirits.composeapp.generated.resources.Res

class GithubJsonRemoteConfigDataSource(private val httpClient: HttpClient) : RemoteConfigDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private var activeClassStats: Map<String, CharacterClassStatsEntity> = emptyMap()
    private var activeAbilityConfig: AbilityConfigEntity = AbilityConfigEntity.fromDomain(config = AbilityConfig.DEFAULT)
    private var activeClassCopy: Map<String, AbilityCopyEntity> = emptyMap()
    private var activeTravelerNames: List<String> = listOf(FALLBACK_TRAVELER_NAME)
    private var activeCredits: CreditsEntity = CreditsEntity.EMPTY
    private var activeVersionGate: VersionGateEntity = VersionGateEntity.UNGATED
    private var activeBugReportConfig: BugReportConfigEntity = BugReportConfigEntity.EMPTY

    override suspend fun initialize() {
        loadBundledDefaults()
        applyRemoteOverrides()
    }

    private suspend fun loadBundledDefaults() {
        activeClassStats =
            loadBundled(fileName = CHARACTER_CLASS_STATS_FILE, fallback = activeClassStats)
                .ifEmpty { activeClassStats }
        activeAbilityConfig = loadBundled(fileName = ABILITY_CONFIG_FILE, fallback = activeAbilityConfig)
        activeClassCopy =
            loadBundled(fileName = CHARACTER_CLASS_COPY_FILE, fallback = activeClassCopy)
                .ifEmpty { activeClassCopy }
        activeTravelerNames =
            loadBundled(fileName = RANDOM_TRAVELER_NAMES_FILE, fallback = activeTravelerNames)
                .ifEmpty { activeTravelerNames }
        activeCredits = loadBundled(fileName = CREDITS_FILE, fallback = activeCredits)
        activeVersionGate = loadBundled(fileName = VERSION_GATE_FILE, fallback = activeVersionGate)
        activeBugReportConfig =
            loadBundled(fileName = BUG_REPORT_FILE, fallback = activeBugReportConfig)
    }

    private suspend fun applyRemoteOverrides() {
        activeClassStats = fetchedOrKept(
            fileName = CHARACTER_CLASS_STATS_FILE,
            current = activeClassStats,
            isFetchedUsable = { it.isNotEmpty() },
        )
        activeAbilityConfig = fetchedOrKept(fileName = ABILITY_CONFIG_FILE, current = activeAbilityConfig)
        activeClassCopy = fetchedOrKept(
            fileName = CHARACTER_CLASS_COPY_FILE,
            current = activeClassCopy,
            isFetchedUsable = { it.isNotEmpty() },
        )
        activeTravelerNames = fetchedOrKept(
            fileName = RANDOM_TRAVELER_NAMES_FILE,
            current = activeTravelerNames,
            isFetchedUsable = { it.isNotEmpty() },
        )
        activeCredits = fetchedOrKept(fileName = CREDITS_FILE, current = activeCredits)
        activeVersionGate = fetchedOrKept(fileName = VERSION_GATE_FILE, current = activeVersionGate)
        activeBugReportConfig =
            fetchedOrKept(fileName = BUG_REPORT_FILE, current = activeBugReportConfig)
    }

    private suspend inline fun <reified T> fetchedOrKept(
        fileName: String,
        current: T,
        isFetchedUsable: (T) -> Boolean = { true },
    ): T {
        val fetched = fetchRemote<T>(fileName = fileName)
        val isFetchAccepted = fetched != null && isFetchedUsable(fetched)
        return if (isFetchAccepted) {
            fetched
        } else {
            current
        }
    }

    private suspend inline fun <reified T> loadBundled(fileName: String, fallback: T): T = runCatching {
        val bytes = Res.readBytes(path = BUNDLED_DIRECTORY + fileName)
        json.decodeFromString<T>(string = bytes.decodeToString())
    }
        .getOrDefault(defaultValue = fallback)

    private suspend inline fun <reified T> fetchRemote(fileName: String): T? = runCatching {
        val body = httpClient.get(urlString = RAW_BASE_URL + fileName).bodyAsText()
        json.decodeFromString<T>(string = body)
    }
        .getOrNull()

    override fun characterClassStats(): Map<CharacterClassType, CharacterClassStatsEntity> = activeClassStats
        .mapNotNull { (name, stats) ->
            runCatching { CharacterClassType.valueOf(value = name) }
                .getOrNull()
                ?.let { it to stats }
        }
        .toMap()

    override fun abilityConfig(): AbilityConfigEntity = activeAbilityConfig

    override fun characterClassCopy(): Map<String, AbilityCopyEntity> = activeClassCopy

    override fun randomTravelerNames(): List<String> = activeTravelerNames

    override fun credits(): CreditsEntity = activeCredits

    override fun minimumSupportedVersionCode(): Long = activeVersionGate.minimumSupportedVersionCode

    override fun latestVersionCode(): Long = activeVersionGate.latestVersionCode

    override fun bugReportEndpointUrl(): String = activeBugReportConfig.endpointUrl

    override fun bugReportToken(): String = activeBugReportConfig.token

    companion object {
        const val RAW_BASE_URL =
            "https://raw.githubusercontent.com/OliverGraham/sorcerers-and-spirits-config/main/"
        const val BUNDLED_DIRECTORY = "files/remote_config/"

        const val CHARACTER_CLASS_STATS_FILE = "character_class_stats.json"
        const val ABILITY_CONFIG_FILE = "ability_config.json"
        const val CHARACTER_CLASS_COPY_FILE = "character_class_copy.json"
        const val RANDOM_TRAVELER_NAMES_FILE = "random_traveler_names.json"
        const val CREDITS_FILE = "credits.json"
        const val VERSION_GATE_FILE = "version_gate.json"
        const val BUG_REPORT_FILE = "bug_report.json"

        const val FALLBACK_TRAVELER_NAME = "a mysterious traveler"
    }
}
