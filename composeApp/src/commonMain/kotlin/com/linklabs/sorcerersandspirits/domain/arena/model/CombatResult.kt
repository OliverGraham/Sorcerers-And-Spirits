package com.linklabs.sorcerersandspirits.domain.arena.model

// TODO Arena: Look at this - seems good but double check
sealed interface CombatResult {
    val messages: List<OutgoingArenaMessage>
    val nextTurnInfo: TurnInfo?

    data class Miss(
        override val messages: List<OutgoingArenaMessage>,
        override val nextTurnInfo: TurnInfo,
    ) : CombatResult

    data class Hit(
        override val messages: List<OutgoingArenaMessage>,
        override val nextTurnInfo: TurnInfo? = null,
        val targetId: String,
    ) : CombatResult

    data class DamageDealt(
        override val messages: List<OutgoingArenaMessage>,
        override val nextTurnInfo: TurnInfo,
        val targetId: String,
        val newHp: Int,
    ) : CombatResult

    data class TargetDefeated(
        override val messages: List<OutgoingArenaMessage>,
        override val nextTurnInfo: TurnInfo,
        val defeatedPlayerId: String,
    ) : CombatResult

    data class Victory(
        override val messages: List<OutgoingArenaMessage>,
        override val nextTurnInfo: TurnInfo? = null,
        val winnerClassName: String,
    ) : CombatResult
}
