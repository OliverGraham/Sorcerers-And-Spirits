package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

interface RemoteConfigDataSource {

    suspend fun initialize()

    fun characterClassStats(): Map<CharacterClassType, CharacterClassStatsEntity>

    fun abilityConfig(): AbilityConfigEntity

    /**
     * The currently active class stat + ability copy, keyed by [ClassCopyKeys]. Falls back to the
     * in-app defaults (the bundled copy) when nothing has been fetched or the payload can't be
     * parsed.
     */
    fun characterClassCopy(): Map<String, AbilityCopyEntity>

    /**
     * The currently active pool of random traveler names. Falls back to the in-app defaults when
     * nothing has been fetched or the payload can't be parsed.
     */
    fun randomTravelerNames(): List<String>

    /**
     * The currently active credits roll. Falls back to the in-app defaults when nothing has been
     * fetched or the payload can't be parsed.
     */
    fun credits(): CreditsEntity

    /**
     * The lowest build version code still allowed to run. Falls back to [UNGATED_VERSION] (which
     * never gates any client) when nothing has been fetched.
     */
    fun minimumSupportedVersionCode(): Long

    /**
     * The latest published build version code. Falls back to [UNGATED_VERSION] when nothing has
     * been fetched.
     */
    fun latestVersionCode(): Long

    fun bugReportEndpointUrl(): String

    fun bugReportToken(): String

    fun appleRevokeEndpointUrl(): String

    fun appleRevokeToken(): String

    companion object {
        // A 0 floor/ceiling means every real build (version code >= 1) compares as up to date, so a
        // missing or failed config can never strand clients behind an upgrade wall.
        const val UNGATED_VERSION = 0L
    }
}
