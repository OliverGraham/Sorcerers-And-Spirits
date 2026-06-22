package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats

class UpdateProfileStatsUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
    private val unlockAchievementsUseCase: UnlockAchievementsUseCase,
) {
    /**
     * Issues the increment for the signed-in user, returning whether the write was made. A caller
     * holding a durable copy of [delta] (see
     * [com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore]) uses the result to
     * decide whether the local copy can be discarded or must be kept for replay.
     *
     * Once the new totals are written, achievements are re-evaluated so any newly-earned ones
     * unlock (SS-074).
     */
    suspend operator fun invoke(delta: ProfileStats): Boolean {
        if (delta.isEmpty) return false

        val userId = authenticationRepository.getUserId() ?: return false

        profileRepository.incrementStats(userId = userId, delta = delta)
        unlockAchievementsUseCase(userId = userId)
        return true
    }
}
