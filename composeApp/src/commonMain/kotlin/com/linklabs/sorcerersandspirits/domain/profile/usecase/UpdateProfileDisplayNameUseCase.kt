package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class UpdateProfileDisplayNameUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String) {
        val userId = authenticationRepository.getUserId() ?: return

        profileRepository.updateDisplayName(userId = userId, newDisplayName = displayName)
    }
}
