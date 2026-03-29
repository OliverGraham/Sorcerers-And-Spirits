package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class UpdateChallengerStatusUseCase(private val tavernRepository: TavernRepository) {
    suspend operator fun invoke(challengeId: String, userId: String, status: ChallengerStatus) {
        tavernRepository.updateChallengerStatus(
            challengeId = challengeId,
            userId = userId,
            status = status,
        )
    }
}
