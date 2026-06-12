package com.linklabs.sorcerersandspirits.domain.arena.model

/** The next action for the user (Domain -> Data -> Domain -> UI) */
sealed interface NextCombatAction {
    val activeUserIds: Set<String>

    data class Idle(override val activeUserIds: Set<String>) : NextCombatAction

    data class NormalAttack(
        val roll: NormalAttackRoll,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : NextCombatAction

    data class AbilityRoll(
        val purpose: AbilityAction,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : NextCombatAction
}
