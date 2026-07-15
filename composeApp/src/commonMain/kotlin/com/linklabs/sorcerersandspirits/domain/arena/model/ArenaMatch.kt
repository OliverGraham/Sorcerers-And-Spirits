package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

sealed interface ArenaMatch {
    val messages: List<IncomingArenaMessage>
    val numberOfNewMessages: Int
    val playerMap: Map<String, ArenaPlayer>
    val matchNumberInGauntlet: Int

    data class WaitingForAllPlayers(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatch

    data class ChoosingClasses(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
        val activePlayer: ArenaPlayer,
        val activePlayerUserId: String,
    ) : ArenaMatch

    data class Combat(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer.Selected>,
        override val matchNumberInGauntlet: Int = 1,
        val nextCombatAction: NextCombatAction,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val doublesBonus: DoublesBonus?,
        val isMirrorImagePenaltyActive: Boolean,
        val myDiceRoll: MyDiceRoll?,
        val participants: Map<String, CharacterClassType>,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<IncomingArenaMessage>,
        override val numberOfNewMessages: Int,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
        val participants: Map<String, CharacterClassType>,
    ) : ArenaMatch
}
