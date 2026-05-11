package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface ArenaMatch {
    val messages: List<ArenaMessage>
    val numberOfNewMessages: Int

    data class WaitingForAllPlayers(
        override val messages: List<ArenaMessage>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatch

    data class ChoosingClasses(
        override val messages: List<ArenaMessage>,
        override val numberOfNewMessages: Int,
        val activePlayer: ArenaPlayer,
        val activePlayerUserId: String,
        val playerMap: Map<String, ArenaPlayer>,
    ) : ArenaMatch

    data class Combat(
        override val messages: List<ArenaMessage>,
        override val numberOfNewMessages: Int,
        val activePlayer: ArenaPlayer.Selected,
        val activePlayerUserId: String,
        val playerMap: Map<String, ArenaPlayer.Selected>,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<ArenaMessage>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatch
}
