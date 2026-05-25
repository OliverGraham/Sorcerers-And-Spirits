package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import com.linklabs.sorcerersandspirits.domain.arena.utilities.PerformCombatAction

data class CombatActionArguments(
    val arenaId: String,
    val attacker: ArenaPlayer.Selected,
    val defender: ArenaPlayer.Selected,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val turnInfo: TurnInfo,
) {
    val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    val attackerDefensiveAbility = attacker.characterClass.defensiveAbility
    val defenderOffensiveAbility = defender.characterClass.offensiveAbility
    val defenderDefensiveAbility = defender.characterClass.defensiveAbility

    val roundNumber = turnInfo.roundNumber
}

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(arguments: CombatActionArguments, currentStep: CombatStep) {
        val results = PerformCombatAction(arguments = arguments, currentStep = currentStep).invoke()

        // This results variable is intended for the repository call in the future.
        println(message = "Combat results: $results")
    }
}
