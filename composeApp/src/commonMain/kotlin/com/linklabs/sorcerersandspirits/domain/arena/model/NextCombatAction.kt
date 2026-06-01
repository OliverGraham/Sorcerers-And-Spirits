package com.linklabs.sorcerersandspirits.domain.arena.model

/** The next action for the user (Domain -> Data -> Domain -> UI) */
sealed interface NextCombatAction {

    data class Idle(val activeUserId: String) : NextCombatAction

    data class NormalAttack(val roll: NormalAttackRoll) : NextCombatAction

    data class AbilityRoll(val purpose: AbilityAction) : NextCombatAction
}
