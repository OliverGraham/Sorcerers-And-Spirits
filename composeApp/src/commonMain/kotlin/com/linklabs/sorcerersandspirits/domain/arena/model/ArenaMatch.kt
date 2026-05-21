package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.ui.screens.arena.model.DiceRoll

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
        val turnInfo: TurnInfo,
        val currentDiceRoll: DiceRoll?,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatch
}
