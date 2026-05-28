package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        val outcome =
            PerformCombatAction(arguments = arguments).invoke(currentAction = currentAction)

        arenaRepository.applyCombatResult(arenaId = arguments.arenaId, result = outcome)
    }
}
