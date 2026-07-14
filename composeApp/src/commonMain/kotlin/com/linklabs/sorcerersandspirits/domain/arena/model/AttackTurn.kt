package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface AttackTurn {
    val attackerUserId: String
    val turnOwnerUserId: String

    data class Standard(override val attackerUserId: String) : AttackTurn {
        override val turnOwnerUserId = attackerUserId
    }

    data class RiposteReversal(
        override val attackerUserId: String,
        override val turnOwnerUserId: String,
    ) : AttackTurn
}
