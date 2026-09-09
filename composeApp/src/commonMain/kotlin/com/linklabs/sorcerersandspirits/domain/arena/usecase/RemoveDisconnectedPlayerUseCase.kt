package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository

class RemoveDisconnectedPlayerUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String, userId: String) {
        arenaRepository.exitArena(arenaId = arenaId, userId = userId)
    }
}
