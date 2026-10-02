package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.NextCombatActionEntity
import com.linklabs.sorcerersandspirits.data.arena.model.resetForNewMatch
import com.linklabs.sorcerersandspirits.data.arena.model.toInitialParticipantMap
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.nullOnTransactionDecodeCrashSerializer
import dev.gitlive.firebase.database.DatabaseReference

internal class StayForAnotherMatch(private val userId: String) {

    suspend operator fun invoke(arenaReference: DatabaseReference) {
        val strategy =
            nullOnTransactionDecodeCrashSerializer(serializer = ArenaMatchEntity.serializer())
        val messagesReference = arenaReference.child(path = ArenaMatchEntity.newMessagesName)

        arenaReference.runTransaction(strategy = strategy) { match ->
            match.stayForAnotherMatchTransaction(messagesReference = messagesReference)
        }
    }

    private fun ArenaMatchEntity?.stayForAnotherMatchTransaction(
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity? {
        val match = this ?: return null
        if (match !is ArenaMatchEntity.Finished) return match

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
            val readiedMessage =
                ArenaMessageEntity.ReadiedForNextMatch(
                    id = messagesReference.addNewKey(),
                    displayName = currentPlayer.displayName,
                )
            match.withUpdatedPlayers(
                updatedPlayers = updatedPlayers,
                newMessages = listOf(element = readiedMessage),
            )
        }
    }

    private fun ArenaMatchEntity.startNextMatch(
        players: Map<String, ArenaPlayerEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity? {
        val isNewGauntlet = matchNumberInGauntlet >= Gauntlet.MATCHES_PER_GAUNTLET

        return if (isNewGauntlet) {
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
                startingCharacterClass = fastestPlayer.characterClassEntity,
            )

        return ArenaMatchEntity.Combat(
            playerEntitiesMap = freshPlayers,
            newMessages = newMessages,
            nextCombatActionEntity =
            NextCombatActionEntity.IdleEntity(
                activeUserIds = setOf(element = fastestPlayer.userId),
            ),
            roundNumber = 1,
            isNewRound = true,
            matchNumberInGauntlet = matchNumberInGauntlet + 1,
            participantEntitiesMap = freshPlayers.toInitialParticipantMap(),
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
            newMessages = emptyList(),
            matchNumberInGauntlet = 1,
        )
    }

    private fun ArenaMatchEntity.Finished.withUpdatedPlayers(
        updatedPlayers: Map<String, ArenaPlayerEntity>,
        newMessages: List<ArenaMessageEntity>,
    ): ArenaMatchEntity = copy(playerEntitiesMap = updatedPlayers, newMessages = newMessages)
}
