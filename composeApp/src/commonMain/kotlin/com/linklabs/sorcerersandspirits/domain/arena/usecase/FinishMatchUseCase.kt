package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository

class FinishMatchUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String) {
        arenaRepository.finishMatch(arenaId = arenaId)
    }
}
