package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

class ChooseCharacterClassUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(arenaId: String, arenaPlayer: ArenaPlayer, nextPlayerId: String?) {
        arenaRepository.chooseCharacterClass(
            arenaId = arenaId,
            arenaPlayer = arenaPlayer,
            nextPlayerId = nextPlayerId,
        )
    }
}
