package com.linklabs.sorcerersandspirits.domain.traveler

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class UpdateTravelerDisplayNameUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val travelerRepository: TravelerRepository,
) {
    suspend operator fun invoke(displayName: String) {
        val userId = authenticationRepository.getUserId() ?: return

        travelerRepository.updateDisplayName(id = userId, displayName = displayName)
    }
}
