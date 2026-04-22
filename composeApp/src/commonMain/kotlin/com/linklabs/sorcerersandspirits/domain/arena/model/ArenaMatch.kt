package com.linklabs.sorcerersandspirits.domain.arena.model

data class ArenaMatch(
    val phase: ArenaPhase,
    val playerMap: Map<String, ArenaPlayer>,
    val activePlayerUserId: String?,
    // TODO Arena: Arena
) {
    val activePlayer = playerMap[activePlayerUserId]
}
