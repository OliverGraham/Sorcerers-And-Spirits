package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats

class UpdateProfileStatsUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(delta: ProfileStats) {
        if (delta.isEmpty) return

        val userId = authenticationRepository.getUserId() ?: return

        profileRepository.incrementStats(userId = userId, delta = delta)
    }
}
