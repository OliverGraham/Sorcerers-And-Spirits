package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        val results =
            PerformCombatAction(arguments = arguments).invoke(currentAction = currentAction)

        // This results variable is intended for the repository call in the future.
        println(message = "Combat results: $results")
    }
}
