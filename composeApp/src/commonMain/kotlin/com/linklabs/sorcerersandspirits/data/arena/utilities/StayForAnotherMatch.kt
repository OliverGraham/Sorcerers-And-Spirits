package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.NextCombatActionEntity
import com.linklabs.sorcerersandspirits.data.arena.model.resetForNewMatch
import com.linklabs.sorcerersandspirits.data.arena.model.toParticipants
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

internal class StayForAnotherMatch(private val userId: String) {
    suspend operator fun invoke(arenaReference: DatabaseReference) {
        val strategy =
            nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())
        val messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName)

        arenaReference.runTransaction(strategy = strategy) { match ->
            match.stayForAnotherMatchTransaction(messagesReference = messagesReference)
        }
    }

    private fun ArenaMatchEntity?.stayForAnotherMatchTransaction(
        messagesReference: DatabaseReference
    ): ArenaMatchEntity? {
        val match = this ?: return null

        val updatedPlayers = match.playerEntitiesMap.toMutableMap()
        val currentPlayer = updatedPlayers[userId] ?: return match

        updatedPlayers[userId] =
            when (currentPlayer) {
                is ArenaPlayerEntity.Selected -> currentPlayer.copy(isReadyForNextMatch = true)
                is ArenaPlayerEntity.Unselected -> currentPlayer.copy(isReadyForNextMatch = true)
            }

        val areAllReady = updatedPlayers.values.all { it.isReadyForNextMatch }

        return if (areAllReady) {
            match.startNextMatch(players = updatedPlayers, messagesReference = messagesReference)
        } else {
            match.withUpdatedPlayers(updatedPlayers = updatedPlayers)
        }
    }

    private fun ArenaMatchEntity.startNextMatch(
        players: Map<String, ArenaPlayerEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity? {
        val isFinalMatch = matchNumberInGauntlet >= Gauntlet.MATCHES_PER_GAUNTLET

        return if (isFinalMatch) {
            startNewGauntlet(players = players)
        } else {
            startNextGauntletMatch(players = players, messagesReference = messagesReference)
        }
    }

    private fun ArenaMatchEntity.startNextGauntletMatch(
        players: Map<String, ArenaPlayerEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity? {
        val freshPlayers =
            players.values.filterIsInstance<ArenaPlayerEntity.Selected>().associate { player ->
                player.userId to
                    player.copy(
                        isReadyForNextMatch = false,
                        characterClassEntity = player.characterClassEntity.resetForNewMatch(),
                    )
            }

        if (freshPlayers.size < 2) return null

        val fastestPlayer = freshPlayers.getFastestPlayer()
        val newMessages =
            messagesReference.generateInitialCombatRoundMessages(
                startingCharacterClass = fastestPlayer.characterClassEntity
            )

        // CHANGE: numberOfNewMessages removed; `messages` is the fresh batch (replace).
        // NEW-MATCH BOUNDARY: this starts the next match in the gauntlet (matchNumberInGauntlet+1).
        // This (and startNewGauntlet below) is the point where on-device stored messages should be
        // CLEARED. QUESTION: clear it here on the RTDB write side (can't — store is per-device), or
        // detect the match-boundary in CoordinateArenaMatchUseCase (matchNumberInGauntlet changed /
        // roundNumber back to 1 + isNewRound) and clear the local store there? Leaning: the latter.
        // Don't worry about clearing the messages, but likely when a match starts is when they should
        // be cleared.
        return ArenaMatchEntity.Combat(
            playerEntitiesMap = freshPlayers,
            messages = newMessages,
            nextCombatActionEntity =
                NextCombatActionEntity.IdleEntity(
                    activeUserIds = setOf(element = fastestPlayer.userId)
                ),
            roundNumber = 1,
            isNewRound = true,
            matchNumberInGauntlet = matchNumberInGauntlet + 1,
            participants = freshPlayers.toParticipants(),
        )
    }

    private fun startNewGauntlet(players: Map<String, ArenaPlayerEntity>): ArenaMatchEntity? {
        val resetPlayers =
            players.mapValues { (_, player) ->
                ArenaPlayerEntity.Unselected(
                    userId = player.userId,
                    displayName = player.displayName,
                )
            }

        if (resetPlayers.size < 2) return null

        // CHANGE: numberOfNewMessages removed. NEW-GAUNTLET BOUNDARY: also a store-clear point.
        return ArenaMatchEntity.ChoosingClasses(
            playerEntitiesMap = resetPlayers,
            activePlayerUserId = resetPlayers.keys.random(),
            messages = emptyList(),
            matchNumberInGauntlet = 1,
        )
    }

    private fun ArenaMatchEntity.withUpdatedPlayers(
        updatedPlayers: Map<String, ArenaPlayerEntity>
    ): ArenaMatchEntity =
        when (this) {
            is ArenaMatchEntity.WaitingForAllPlayers -> copy(playerEntitiesMap = updatedPlayers)

            is ArenaMatchEntity.ChoosingClasses -> copy(playerEntitiesMap = updatedPlayers)

            is ArenaMatchEntity.Combat ->
                copy(
                    playerEntitiesMap =
                        updatedPlayers.mapValues { it.value as ArenaPlayerEntity.Selected }
                )

            is ArenaMatchEntity.Finished -> copy(playerEntitiesMap = updatedPlayers)
        }
}
