package com.linklabs.sorcerersandspirits.domain.profile

/**
 * Durable local record of the stat delta that has been computed but not yet acknowledged by the
 * remote profile store. It closes the window Firestore's offline persistence does not cover: a
 * delta accumulated in memory during a match but never issued as a write before the process dies
 * (SS-076).
 *
 * The in-progress session delta is [save]d here as it grows, so a crash or kill loses at most the
 * current in-progress match. The record is [clear]ed only once the delta has been handed to the
 * remote store; on next launch any surviving record is [load]ed and replayed, giving stat writes an
 * at-least-once guarantee.
 */
interface PendingStatsStore {
    suspend fun load(): ProfileStats

    suspend fun save(delta: ProfileStats)

    suspend fun clear()
}
