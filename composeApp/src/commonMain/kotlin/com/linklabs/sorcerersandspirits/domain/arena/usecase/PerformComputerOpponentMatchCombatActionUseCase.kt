package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.local.LocalArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionArguments

class PerformComputerOpponentMatchCombatActionUseCase(
    private val localArenaRepository: LocalArenaRepository,
) {
    suspend operator fun invoke(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        localArenaRepository.performCombatAction(
            arguments = arguments,
            currentAction = currentAction,
        )
    }
}
