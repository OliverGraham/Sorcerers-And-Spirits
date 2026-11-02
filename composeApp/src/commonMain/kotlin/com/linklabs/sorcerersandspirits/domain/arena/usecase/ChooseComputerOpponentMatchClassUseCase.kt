package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.local.LocalArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

class ChooseComputerOpponentMatchClassUseCase(
    private val localArenaRepository: LocalArenaRepository,
) {
    suspend operator fun invoke(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        allPlayers: List<ArenaPlayer>,
    ) {
        localArenaRepository.chooseCharacterClass(
            selectedArenaPlayer = selectedArenaPlayer,
            allPlayers = allPlayers,
        )
    }
}
