package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class CreateProfileUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String) {
        val userId = authenticationRepository.signInAnonymously() ?: return

        profileRepository.createProfile(
            profile = Profile(userId = userId, displayName = displayName)
        )
    }
}
