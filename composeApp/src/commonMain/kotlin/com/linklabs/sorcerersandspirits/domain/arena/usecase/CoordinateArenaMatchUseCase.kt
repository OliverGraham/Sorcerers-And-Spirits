package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
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
    ): Flow<ArenaMatch?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)
        val profile = profileRepository.getProfile(userId = userId) ?: return flowOf(value = null)

        val isUserConnected =
            arenaRepository.connectToArena(
                arenaId = arenaId,
                expectedNumberOfPlayers = expectedNumberOfPlayers,
                userId = userId,
                displayName = profile.displayName,
            )

        return if (isUserConnected) {
            // TODO: Why isn't this part of connectToArena? Should be one operation, right?
            arenaRepository.connectArenaPresence(arenaId = arenaId, userId = userId)
            arenaRepository.getArenaMatchFlow(arenaId = arenaId)
        } else {
            flowOf(value = null)
        }
    }
}
