package com.linklabs.sorcerersandspirits.domain.arena.model

/** The next action for the user (Domain -> Data -> Domain -> UI) */
sealed interface NextCombatAction {
    val activeUserIds: Set<String>

    data class Idle(override val activeUserIds: Set<String>) : NextCombatAction

    data class NormalAttack(val roll: NormalAttackRoll, override val activeUserIds: Set<String>) :
        NextCombatAction

    data class AbilityRoll(val purpose: AbilityAction, override val activeUserIds: Set<String>) :
        NextCombatAction
}
