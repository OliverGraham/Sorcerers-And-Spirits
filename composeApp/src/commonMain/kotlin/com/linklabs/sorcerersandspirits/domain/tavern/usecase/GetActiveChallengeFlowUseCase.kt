package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.ActiveChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Emits the current user's active challenge so the challenge dialog can be hosted above navigation
 * and shown on any screen. Separate from [GetTavernDataFlowUseCase] so a challenge can be observed
 * without also subscribing to the tavern travelers list.
 */
class GetActiveChallengeFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {

    operator fun invoke(): Flow<ActiveChallenge?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)

        return tavernRepository
            .getActiveChallengeFlow(userId = userId)
            .distinctUntilChanged()
            .map { challenge -> ActiveChallenge(userId = userId, challenge = challenge) }
    }
}
