package com.linklabs.sorcerersandspirits.domain.arena.model

/** The next action for the user (Domain -> Data -> Domain -> UI) */
sealed interface NextCombatAction {
    val activeUserIds: Set<String>

    data class Idle(override val activeUserIds: Set<String>) : NextCombatAction

    data class NormalAttack(
        val roll: NormalAttackType,
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

fun NextCombatAction.turnOwnerUserId(): String =
    when (this) {
        is NextCombatAction.Idle -> activeUserIds.first()
        is NextCombatAction.NormalAttack -> originalTurnUserId
        is NextCombatAction.AbilityRoll -> originalTurnUserId
    }

fun NextCombatAction.lockedTargetUserId(userId: String): String? {
    val attack = this as? NextCombatAction.NormalAttack ?: return null
    return if (attack.attackerUserId == userId) attack.defenderUserId else null
}
