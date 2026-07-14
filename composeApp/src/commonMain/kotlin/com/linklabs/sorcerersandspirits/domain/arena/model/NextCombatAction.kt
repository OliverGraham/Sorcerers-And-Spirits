package com.linklabs.sorcerersandspirits.domain.arena.model

/** The next action for the user (Domain -> Data -> Domain -> UI) */
sealed interface NextCombatAction {
    val activeUserIds: Set<String>

    data class Idle(override val activeUserIds: Set<String>) : NextCombatAction

    data class NormalAttack(
        val roll: NormalAttackType,
        val attackTurn: AttackTurn,
        val defenderUserId: String,
        override val activeUserIds: Set<String>,
    ) : NextCombatAction

    data class AbilityRoll(
        val purpose: AbilityAction,
        val attackTurn: AttackTurn,
        val defenderUserId: String,
        override val activeUserIds: Set<String>,
    ) : NextCombatAction
}

fun NextCombatAction.turnOwnerUserId(): String =
    when (this) {
        is NextCombatAction.Idle -> activeUserIds.first()
        is NextCombatAction.NormalAttack -> attackTurn.turnOwnerUserId
        is NextCombatAction.AbilityRoll -> attackTurn.turnOwnerUserId
    }

fun NextCombatAction.lockedTargetUserId(userId: String): String? {
    val attack = this as? NextCombatAction.NormalAttack ?: return null
    return if (attack.attackTurn.attackerUserId == userId) attack.defenderUserId else null
}
