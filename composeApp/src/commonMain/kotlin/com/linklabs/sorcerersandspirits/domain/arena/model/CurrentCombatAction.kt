package com.linklabs.sorcerersandspirits.domain.arena.model

/** Action the user is taking (UI -> Domain (then is transformed into [NextCombatAction]) */
sealed interface CurrentCombatAction {
    val attackTurn: AttackTurn
    val defenderUserId: String

    data class NormalAttack(
        val roll: NormalAttackType,
        override val attackTurn: AttackTurn,
        override val defenderUserId: String,
    ) : CurrentCombatAction

    data class AbilityRoll(
        val purpose: AbilityAction,
        override val attackTurn: AttackTurn,
        override val defenderUserId: String,
        val rollingUserId: String,
    ) : CurrentCombatAction
}
