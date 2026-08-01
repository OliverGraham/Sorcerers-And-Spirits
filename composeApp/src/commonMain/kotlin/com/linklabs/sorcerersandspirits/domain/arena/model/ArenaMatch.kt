package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

sealed interface ArenaMatch {
    // CHANGE: `messages` is now the FULL accumulated on-device log (assembled by
    // CoordinateArenaMatchUseCase from the local store), and replaces numberOfNewMessages with
    // `newMessages` = the batch that just arrived from RTDB (what the UI animates). Apply to every
    // subtype below. QUESTION: newMessages on the shared interface (all states) — yes? Non-combat
    // states just carry their setup batch (or empty). Confirm.
    // Yes - every state can have the messages.

    val messages: List<IncomingArenaMessage>
    val numberOfNewMessages: Int

    // You wrote code - you weren't supposed to. I approve, however
    // val newMessages: List<IncomingArenaMessage>
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
        val isMatchOver: Boolean,
        val diceRoll: DiceRoll?,
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
