package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RealDiceRoller

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(
        arguments: CombatActionArguments,
        currentAction: CurrentCombatAction,
    ) {
        val diceRoller = RealDiceRoller(initialBonus = arguments.doublesBonus)
        val outcome =
            PerformCombatAction(arguments = arguments, diceRoller = diceRoller)
                .invoke(currentAction = currentAction)

        arenaRepository.applyCombatResult(arenaId = arguments.arenaId, result = outcome)
    }
}
