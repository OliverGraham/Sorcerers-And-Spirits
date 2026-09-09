package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import kotlinx.coroutines.flow.Flow

class GetArenaPresenceFlowUseCase(private val arenaRepository: ArenaRepository) {
    operator fun invoke(arenaId: String): Flow<Map<String, Boolean>> = arenaRepository.getArenaPresenceFlow(arenaId = arenaId)
}
