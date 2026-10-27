package com.linklabs.sorcerersandspirits.domain.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

sealed interface ArenaMatch {
    val messages: List<IncomingArenaMessage>
    val playerMap: Map<String, ArenaPlayer>
    val matchNumberInGauntlet: Int

    data class WaitingForAllPlayers(
        override val messages: List<IncomingArenaMessage>,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatch

    data class ChoosingClasses(
        override val messages: List<IncomingArenaMessage>,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
        val activePlayer: ArenaPlayer,
        val activePlayerUserId: String,
    ) : ArenaMatch

    data class Combat(
        override val messages: List<IncomingArenaMessage>,
        override val playerMap: Map<String, ArenaPlayer.Selected>,
        override val matchNumberInGauntlet: Int = 1,
        val nextCombatAction: NextCombatAction,
        val currentTargetUserId: String?,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val isMatchOver: Boolean,
        val diceRoll: DiceRoll?,
        val participantMap: Map<String, MatchParticipant>,
    ) : ArenaMatch

    data class Finished(
        override val messages: List<IncomingArenaMessage>,
        override val playerMap: Map<String, ArenaPlayer>,
        override val matchNumberInGauntlet: Int = 1,
        val participantMap: Map<String, MatchParticipant>,
    ) : ArenaMatch {
        val hasEnoughPlayersToContinue = playerMap.size >= MINIMUM_PLAYERS_TO_CONTINUE
        val isFinalMatch = matchNumberInGauntlet >= Gauntlet.MATCHES_PER_GAUNTLET

        private companion object {
            const val MINIMUM_PLAYERS_TO_CONTINUE = 2
        }
    }
}

// TODO: I don't like this, but it's not a priority. The Messages Header needs this for name and
//  color of whose turn it is. This isn't a clear, direct way of doing it - so can be refactored.
fun ArenaMatch.Combat.turnOwnerClassType(): CharacterClassType {
    val turnOwnerUserId =
        when (val action = nextCombatAction) {
            is NextCombatAction.Idle -> action.activeUserIds.first()
            is NextCombatAction.NormalAttack -> action.attackTurn.turnOwnerUserId
            is NextCombatAction.AbilityRoll -> action.attackTurn.turnOwnerUserId
        }
    return playerMap.getValue(key = turnOwnerUserId).characterClass.type
}
