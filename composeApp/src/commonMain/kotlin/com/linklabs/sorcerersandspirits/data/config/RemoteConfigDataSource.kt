package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.data.tavern.TavernDataSource
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * Seam over Firebase RemoteConfig so the orchestration above it stays unit-testable without the
 * gitlive RemoteConfig client. Mirrors the [TavernDataSource] pattern.
 */
interface RemoteConfigDataSource {

    /** Applies in-app defaults and fetches/activates the latest values. Call once at app start. */
    suspend fun initialize()

    /**
     * The currently active per-class stats. Falls back to the in-app defaults (which equal the
     * shipped class values) when nothing has been fetched or the payload can't be parsed.
     */
    fun characterClassStats(): Map<CharacterClassType, CharacterClassStatsEntity>

    /**
     * The currently active pool of random traveler names. Falls back to the in-app defaults when
     * nothing has been fetched or the payload can't be parsed.
     */
    fun randomTravelerNames(): List<String>
}
