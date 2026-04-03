package com.linklabs.sorcerersandspirits.domain.traveler

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus

class SaveNewTravelerUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val travelerRepository: TravelerRepository,
) {
    suspend operator fun invoke(displayName: String): Traveler? {
        val userId = authenticationRepository.signInAnonymously() ?: return null

        val traveler =
            Traveler(
                userId = userId,
                displayName = displayName,
                tavernStatus = TavernStatus.Available,
            )
        travelerRepository.saveTraveler(traveler = traveler)

        return traveler
    }
}
