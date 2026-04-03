package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository

class EnterTavernUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
    private val travelerRepository: TravelerRepository,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId()
        if (userId.isNullOrEmpty()) return

        val traveler = travelerRepository.getTraveler(id = userId) ?: return

        tavernRepository.connectTravelerPresence(
            userId = userId,
            displayName = traveler.displayName,
        )
    }
}
