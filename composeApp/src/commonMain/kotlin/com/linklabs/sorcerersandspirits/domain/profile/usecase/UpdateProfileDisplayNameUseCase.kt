package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class UpdateProfileDisplayNameUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String): DisplayNameResult {
        val userId = authenticationRepository.getUserId() ?: return DisplayNameResult.ERROR

        return profileRepository.updateDisplayName(userId = userId, newDisplayName = displayName)
    }
}
