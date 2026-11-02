package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.local.LocalArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import kotlinx.coroutines.flow.Flow

class GetComputerOpponentMatchUseCase(private val localArenaRepository: LocalArenaRepository) {
    operator fun invoke(): Flow<ArenaMatch?> = localArenaRepository.matchFlow
}
