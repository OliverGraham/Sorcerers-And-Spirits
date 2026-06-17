package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class UpdateProfileStatsUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(increments: Map<String, Int>) {
        if (increments.isEmpty()) return

        val userId = authenticationRepository.getUserId() ?: return

        profileRepository.incrementStats(userId = userId, increments = increments)
    }
}
