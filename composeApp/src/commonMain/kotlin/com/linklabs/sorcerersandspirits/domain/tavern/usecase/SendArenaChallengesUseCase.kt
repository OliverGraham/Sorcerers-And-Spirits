package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

class SendArenaChallengesUseCase(
    private val tavernRepository: TavernRepository,
) {
    suspend operator fun invoke(sender: Traveler, receivers: List<Traveler>) {
        tavernRepository.sendArenaChallenges(
            sender = sender,
            receivers = receivers,
        )
    }
}
