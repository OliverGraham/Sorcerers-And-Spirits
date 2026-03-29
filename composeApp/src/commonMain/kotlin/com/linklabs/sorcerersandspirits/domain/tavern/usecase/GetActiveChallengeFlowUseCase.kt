package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetActiveChallengeFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    operator fun invoke(): Flow<Challenge?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)

        return tavernRepository.getActiveChallengeFlow(userId = userId)
    }
}
