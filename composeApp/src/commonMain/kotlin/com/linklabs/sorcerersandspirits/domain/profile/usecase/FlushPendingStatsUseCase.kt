package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore

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
