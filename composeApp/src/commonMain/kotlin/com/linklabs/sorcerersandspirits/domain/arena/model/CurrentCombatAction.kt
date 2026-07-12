package com.linklabs.sorcerersandspirits.domain.arena.model

/** Action the user is taking (UI -> Domain (then is transformed into [NextCombatAction]) */
sealed interface CurrentCombatAction {
    val attackerUserId: String
    val defenderUserId: String
    val originalTurnUserId: String

    data class NormalAttack(
        val roll: NormalAttackType,
        override val attackerUserId: String,
        override val defenderUserId: String,
        override val originalTurnUserId: String,
    ) : CurrentCombatAction

    data class AbilityRoll(
        val purpose: AbilityAction,
        override val attackerUserId: String,
        override val defenderUserId: String,
        override val originalTurnUserId: String,
        val rollingUserId: String,
    ) : CurrentCombatAction
}
