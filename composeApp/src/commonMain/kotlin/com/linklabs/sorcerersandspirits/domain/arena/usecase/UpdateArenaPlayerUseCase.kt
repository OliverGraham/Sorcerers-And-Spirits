package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

class UpdateArenaPlayerUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String, arenaPlayer: ArenaPlayer) {
        arenaRepository.updateArenaPlayer(arenaId = arenaId, arenaPlayer = arenaPlayer)
    }
}
