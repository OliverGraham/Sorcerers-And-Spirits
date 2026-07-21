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

        return ArenaMatchEntity.Combat(
            playerEntitiesMap = freshPlayers,
            messages = newMessages,
            numberOfNewMessages = newMessages.size,
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

        return ArenaMatchEntity.ChoosingClasses(
            playerEntitiesMap = resetPlayers,
            activePlayerUserId = resetPlayers.keys.random(),
            messages = emptyList(),
            numberOfNewMessages = 0,
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
