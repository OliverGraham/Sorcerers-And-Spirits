package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

/** One-shot read of the signed-in user's profile, or null if they have none yet. */
class GetProfileUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Profile? {
        val userId = authenticationRepository.getUserId() ?: return null

        return profileRepository.getProfile(userId = userId)
    }
}
