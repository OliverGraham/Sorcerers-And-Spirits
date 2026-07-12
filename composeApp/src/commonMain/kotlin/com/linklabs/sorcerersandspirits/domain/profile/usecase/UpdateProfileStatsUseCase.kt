package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats

class UpdateProfileStatsUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
    private val unlockAchievementsUseCase: UnlockAchievementsUseCase,
) {
    suspend operator fun invoke(delta: ProfileStats): Boolean {
        if (delta.isEmpty) return false

        val userId = authenticationRepository.getUserId() ?: return false

        profileRepository.incrementStats(userId = userId, delta = delta)
        unlockAchievementsUseCase(userId = userId)
        return true
    }
}
