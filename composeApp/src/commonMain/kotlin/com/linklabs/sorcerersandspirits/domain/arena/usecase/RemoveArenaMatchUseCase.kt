package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository

class RemoveArenaMatchUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String) {
        arenaRepository.removeArenaMatch(arenaId = arenaId)
    }
}
