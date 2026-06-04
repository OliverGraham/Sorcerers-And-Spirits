package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface ArenaMatch {
    val messages: List<IncomingArenaMessage>
    val numberOfNewMessages: Int

    data class WaitingForAllPlayers(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatch

    data class ChoosingClasses(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        val activePlayer: ArenaPlayer,
        val activePlayerUserId: String,
        val playerMap: Map<String, ArenaPlayer>,
    ) : ArenaMatch

    data class Combat(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        val activePlayer: ArenaPlayer.Selected,
        val playerMap: Map<String, ArenaPlayer.Selected>,
        val nextCombatAction: NextCombatAction,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val doublesBonus: DoublesBonus?,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatch
}
