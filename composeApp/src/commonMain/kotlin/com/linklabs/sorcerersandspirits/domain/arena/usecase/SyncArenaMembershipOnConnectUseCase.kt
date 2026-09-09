package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class SyncArenaMembershipOnConnectUseCase(
    private val arenaRepository: ArenaRepository,
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId() ?: return
        arenaRepository.syncArenaMembershipOnConnect(userId = userId)
    }
}
