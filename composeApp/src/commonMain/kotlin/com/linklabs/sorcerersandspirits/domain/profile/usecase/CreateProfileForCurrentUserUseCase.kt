package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

/**
 * Creates a profile for the already-signed-in user. Unlike [CreateProfileUseCase], it does NOT sign
 * in anonymously first — used right after a provider sign-in, where re-authenticating would discard
 * the account that was just established.
 */
class CreateProfileForCurrentUserUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String): DisplayNameResult {
        val userId = authenticationRepository.getUserId() ?: return DisplayNameResult.ERROR

        return profileRepository.createProfile(
            profile = Profile(userId = userId, displayName = displayName),
        )
    }
}
