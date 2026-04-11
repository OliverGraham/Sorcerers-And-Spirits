package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class UpdateChallengerStatusUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    suspend operator fun invoke(challengeId: String, status: ChallengerStatus) {
        val userId = authenticationRepository.getUserId() ?: return

        tavernRepository.updateChallengerStatus(
            challengeId = challengeId,
            userId = userId,
            status = status,
        )
    }
}
