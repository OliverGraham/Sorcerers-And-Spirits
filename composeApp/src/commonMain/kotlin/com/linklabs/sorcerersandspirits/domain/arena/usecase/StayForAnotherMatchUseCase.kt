package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class StayForAnotherMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val arenaRepository: ArenaRepository,
) {
    suspend operator fun invoke(arenaId: String) {
        val userId = authenticationRepository.getUserId() ?: return

        arenaRepository.stayForAnotherMatch(arenaId = arenaId, userId = userId)
    }
}
