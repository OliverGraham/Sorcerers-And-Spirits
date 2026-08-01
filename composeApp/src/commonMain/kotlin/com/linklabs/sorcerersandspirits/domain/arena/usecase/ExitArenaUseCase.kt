package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository

class ExitArenaUseCase(
    private val arenaRepository: ArenaRepository,
    private val authenticationRepository: AuthenticationRepository,
    private val tavernRepository: TavernRepository,
) {
    suspend operator fun invoke(arenaId: String) {
        val userId = authenticationRepository.getUserId() ?: return

        arenaRepository.exitArena(arenaId = arenaId, userId = userId)
        arenaRepository.clearArenaMessages(arenaId = arenaId)
        tavernRepository.setTravelerAvailableIfPresent(userId = userId)
    }
}
