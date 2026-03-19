package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class GetArenaChallengeFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    operator fun invoke() =
        tavernRepository.getArenaChallengeFlow(userId = authenticationRepository.getUserId())
}
