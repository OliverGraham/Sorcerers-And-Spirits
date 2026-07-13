package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CoordinateArenaMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(
        arenaId: String,
        expectedNumberOfPlayers: Int,
        computerOpponents: List<ComputerOpponent> = emptyList(),
    ): Flow<ArenaMatch?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)
        val profile = profileRepository.getProfile(userId = userId) ?: return flowOf(value = null)

        computerOpponents.forEach { opponent ->
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = opponent.userId,
                displayName = opponent.displayName,
            )
        }

        val isUserConnected =
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = profile.displayName,
            )

        return if (isUserConnected) {
            arenaRepository.getArenaMatchFlow(arenaId = arenaId)
        } else {
            flowOf(value = null)
        }
    }
}
