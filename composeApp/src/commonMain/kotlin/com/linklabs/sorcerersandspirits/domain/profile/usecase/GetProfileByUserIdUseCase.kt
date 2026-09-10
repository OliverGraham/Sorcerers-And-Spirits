package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class GetProfileByUserIdUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String): Profile? = profileRepository.getProfile(userId = userId)
}
