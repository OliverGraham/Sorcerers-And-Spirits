package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class EnterTavernUseCase(private val tavernRepository: TavernRepository) {
    suspend operator fun invoke(profile: Profile) {
        tavernRepository.connectTravelerPresence(
            userId = profile.userId,
            displayName = profile.displayName,
        )
    }
}
