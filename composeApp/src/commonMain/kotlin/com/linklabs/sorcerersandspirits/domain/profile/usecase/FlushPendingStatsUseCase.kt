package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore

/**
 * Replays any stats delta a previous session persisted but never flushed — typically because the
 * process was killed mid-match (SS-076). Run at launch: the local record is cleared only once the
 * remote write succeeds, so a delta left over while signed out (or otherwise unwritable) survives
 * for a later attempt rather than being silently dropped.
 */
class FlushPendingStatsUseCase(
    private val pendingStatsStore: PendingStatsStore,
    private val updateProfileStatsUseCase: UpdateProfileStatsUseCase,
) {
    suspend operator fun invoke() {
        val pendingDelta = pendingStatsStore.load()
        if (pendingDelta.isEmpty) return

        val wasWritten = updateProfileStatsUseCase(delta = pendingDelta)
        if (wasWritten) pendingStatsStore.clear()
    }
}
