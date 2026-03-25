package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus

class EnterTavernUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
    private val travelerRepository: TravelerRepository,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId()
        if (userId.isNullOrEmpty()) return

        val traveler = getOrCreateTraveler(userId = userId)

        tavernRepository.aTravelerEntersTavern(traveler = traveler)
    }

    private suspend fun getOrCreateTraveler(userId: String): Traveler {
        val existingTraveler = travelerRepository.getTraveler(id = userId)

        return if (existingTraveler == null) {
            val newTraveler =
                Traveler(
                    id = userId,
                    displayName = DEFAULT_DISPLAY_NAME,
                    status = TravelerStatus.Available,
                )
            travelerRepository.saveTraveler(traveler = newTraveler)
            newTraveler
        } else {
            existingTraveler
        }
    }

    companion object {
        private const val DEFAULT_DISPLAY_NAME = "A nameless traveler"
    }
}
