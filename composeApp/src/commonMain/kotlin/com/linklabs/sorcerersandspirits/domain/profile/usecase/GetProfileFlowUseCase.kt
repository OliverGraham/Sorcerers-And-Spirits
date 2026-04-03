package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetProfileFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<Profile?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)

        return profileRepository.getProfileFlow(userId = userId)
    }
}
