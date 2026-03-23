package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

class GetArenaChallengeFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    // TODO: What can be improved here?
    operator fun invoke(): Flow<ArenaChallenge?> {
        val userId = authenticationRepository.getUserId()
        if (userId.isNullOrEmpty()) return flowOf(value = null)

        return tavernRepository.getArenaChallengeFlow(userId = userId).onEach { arenaChallenge ->
            if (arenaChallenge.hasEveryoneAcceptedTheInvite) {
                tavernRepository.removeArenaChallenge(arenaChallenge = arenaChallenge)
            }
        }
    }
}
