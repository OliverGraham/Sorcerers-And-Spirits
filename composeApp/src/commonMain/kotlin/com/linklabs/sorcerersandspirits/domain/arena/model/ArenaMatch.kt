package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface ArenaMatch {
    val messages: List<IncomingArenaMessage>
    val numberOfNewMessages: Int
    val playerMap: Map<String, ArenaPlayer>

    data class WaitingForAllPlayers(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
    ) : ArenaMatch

    data class ChoosingClasses(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
        val activePlayer: ArenaPlayer,
        val activePlayerUserId: String,
    ) : ArenaMatch

    data class Combat(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer.Selected>,
        val nextCombatAction: NextCombatAction,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val doublesBonus: DoublesBonus?,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
    ) : ArenaMatch
}
