package com.linklabs.sorcerersandspirits.domain.arena.model

data class ArenaMatch(
    val phase: ArenaPhase,
    val playerMap: Map<String, ArenaPlayer>,
    val activePlayerUserId: String?,
    val messages: List<ArenaMessage> = emptyList(),
) {
    val activePlayer = playerMap[activePlayerUserId]
}
