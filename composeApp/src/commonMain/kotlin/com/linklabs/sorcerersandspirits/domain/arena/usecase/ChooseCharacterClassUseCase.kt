package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

class ChooseCharacterClassUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(
        arenaId: String,
        selectedArenaPlayer: ArenaPlayer.Selected,
        allPlayers: List<ArenaPlayer>,
    ) {
        arenaRepository.chooseCharacterClass(
            arenaId = arenaId,
            selectedArenaPlayer = selectedArenaPlayer,
            nextPlayerId = determineNextPlayerId(allPlayers = allPlayers),
        )
    }

    private fun determineNextPlayerId(allPlayers: List<ArenaPlayer>): String {
        val (selected, unselected) = allPlayers.partition { it is ArenaPlayer.Selected }

        return if (unselected.isNotEmpty()) {
            unselected.random().userId
        } else {
            selected
                .filterIsInstance<ArenaPlayer.Selected>()
                .maxBy { it.characterClass.speed }
                .userId
        }
    }
}
