package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface NormalAttackRoll {
    data class ArmorClass(
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String = attackerUserId,
    ) : NormalAttackRoll

    data class Health(val attackerUserId: String, val defenderUserId: String) : NormalAttackRoll
}
