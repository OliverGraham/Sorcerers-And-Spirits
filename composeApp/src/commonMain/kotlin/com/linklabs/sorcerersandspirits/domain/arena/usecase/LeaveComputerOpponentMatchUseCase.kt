package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.local.LocalArenaRepository

class LeaveComputerOpponentMatchUseCase(private val localArenaRepository: LocalArenaRepository) {
    operator fun invoke() = localArenaRepository.exit()
}
