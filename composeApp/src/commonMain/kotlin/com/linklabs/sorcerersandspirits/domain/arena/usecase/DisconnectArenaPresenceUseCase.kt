package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository

class DisconnectArenaPresenceUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke() {
        arenaRepository.disconnectArenaPresence()
    }
}
