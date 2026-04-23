package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CoordinateArenaMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
) {
    suspend operator fun invoke(arenaId: String, expectedNumberOfPlayers: Int): Flow<ArenaMatch?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)

        val isUserConnected =
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
            )

        return if (isUserConnected) {
            arenaRepository.getArenaMatchFlow(arenaId = arenaId)
        } else {
            flowOf(value = null)
        }
    }
}
