package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(
        arenaId: String,
        attackerUserId: String,
        targetUserId: String,
        rollType: RollType,
    ) {
        arenaRepository.performCombatAction(
            arenaId = arenaId,
            attackerUserId = attackerUserId,
            targetUserId = targetUserId,
            rollType = rollType,
        )
    }
}
