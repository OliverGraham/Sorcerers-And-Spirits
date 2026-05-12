package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface CombatResult {
    val messages: List<OutgoingArenaMessage>
    val messageStartIndex: Int

    data class Miss(
        override val messages: List<OutgoingArenaMessage>,
        override val messageStartIndex: Int,
        val nextActivePlayerId: String,
    ) : CombatResult

    data class Hit(
        override val messages: List<OutgoingArenaMessage>,
        override val messageStartIndex: Int,
        val targetId: String,
    ) : CombatResult

    data class DamageDealt(
        override val messages: List<OutgoingArenaMessage>,
        override val messageStartIndex: Int,
        val targetId: String,
        val newHp: Int,
        val nextActivePlayerId: String,
    ) : CombatResult

    data class TargetDefeated(
        override val messages: List<OutgoingArenaMessage>,
        override val messageStartIndex: Int,
        val defeatedPlayerId: String,
        val nextActivePlayerId: String,
    ) : CombatResult

    data class Victory(
        override val messages: List<OutgoingArenaMessage>,
        override val messageStartIndex: Int,
        val winnerClassName: String,
    ) : CombatResult
}
