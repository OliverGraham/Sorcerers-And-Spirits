package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class DeleteProfileUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId() ?: return

        profileRepository.deleteProfile(userId = userId)
    }
}
