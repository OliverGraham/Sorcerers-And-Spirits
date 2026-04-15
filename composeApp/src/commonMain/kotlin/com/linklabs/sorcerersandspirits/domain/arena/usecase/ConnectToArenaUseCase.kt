package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class ConnectToArenaUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
) {
    suspend operator fun invoke(arenaId: String) {
        val userId = authenticationRepository.getUserId() ?: return

        arenaRepository.connectToArena(arenaId = arenaId, userId = userId)
    }
}
