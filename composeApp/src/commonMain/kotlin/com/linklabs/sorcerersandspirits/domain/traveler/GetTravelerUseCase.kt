package com.linklabs.sorcerersandspirits.domain.traveler

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class GetTravelerUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val travelerRepository: TravelerRepository,
) {
    suspend operator fun invoke(): Traveler? {
        val userId = authenticationRepository.getUserId() ?: return null

        return travelerRepository.getTraveler(id = userId)
    }
}
