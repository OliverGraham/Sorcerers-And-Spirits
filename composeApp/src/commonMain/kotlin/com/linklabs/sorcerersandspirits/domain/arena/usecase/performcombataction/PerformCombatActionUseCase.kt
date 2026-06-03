package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.DiceRoller

class PerformCombatActionUseCase(
    private val arenaRepository: ArenaRepository,
    private val diceRoller: DiceRoller,
) {

    suspend operator fun invoke(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        val outcome =
            PerformCombatAction(arguments = arguments, diceRoller = diceRoller)
                .invoke(currentAction = currentAction)

        arenaRepository.applyCombatResult(arenaId = arguments.arenaId, result = outcome)
    }
}
