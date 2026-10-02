package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassEntity
import com.linklabs.sorcerersandspirits.data.arena.model.NextCombatActionEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toInitialParticipantMap
import dev.gitlive.firebase.database.DatabaseReference

internal class ChooseCharacterClass(
    private val arenaReference: DatabaseReference,
    private val selectedArenaPlayer: ArenaPlayerEntity.Selected,
    private val nextPlayerId: String,
) {

    /**
     * runTransaction doesn't need a nullable serializer since a document should always be created
     * by the time [invoke] is called.
     */
    suspend operator fun invoke() {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { match ->
            match.chooseCharacterClassTransaction()
        }
    }

    private fun ArenaMatchEntity.chooseCharacterClassTransaction() = when (this) {
        is ArenaMatchEntity.ChoosingClasses -> chooseCharacterClassTransaction()

        is ArenaMatchEntity.WaitingForAllPlayers,
        is ArenaMatchEntity.Combat,
        is ArenaMatchEntity.Finished,
        -> this
    }

    private fun ArenaMatchEntity.ChoosingClasses.chooseCharacterClassTransaction(): ArenaMatchEntity {
        val messageGenerator =
            ChooseCharacterClassMessageGenerator(
                messagesReference = arenaReference.child(path = ArenaMatchEntity.newMessagesName),
            )

        val updatedPlayers = playerEntitiesMap.toMutableMap()
        updatedPlayers[selectedArenaPlayer.userId] = selectedArenaPlayer

        val areAllSelected = updatedPlayers.values.all { it is ArenaPlayerEntity.Selected }

        return if (areAllSelected) {
            transitionToCombat(
                messageGenerator = messageGenerator,
                selectedPlayers =
                updatedPlayers.values.associate {
                    it.userId to it as ArenaPlayerEntity.Selected
                },
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
        } else {
            updateChoosingClasses(
                messageGenerator = messageGenerator,
                updatedPlayers = updatedPlayers,
            )
        }
    }

    private fun transitionToCombat(
        messageGenerator: ChooseCharacterClassMessageGenerator,
        selectedPlayers: Map<String, ArenaPlayerEntity.Selected>,
        matchNumberInGauntlet: Int,
    ): ArenaMatchEntity.Combat {
        val fastestPlayer = selectedPlayers.getFastestPlayer()
        val updatedMessages =
            messageGenerator.generateCombatStartingMessages(
                startingCharacterClass = fastestPlayer.characterClassEntity,
            )

        val fastestPlayerUserId = fastestPlayer.userId
        return ArenaMatchEntity.Combat(
            playerEntitiesMap = selectedPlayers,
            newMessages = updatedMessages,
            nextCombatActionEntity =
            NextCombatActionEntity.IdleEntity(
                activeUserIds = setOf(element = fastestPlayerUserId),
            ),
            roundNumber = 1,
            isNewRound = true,
            matchNumberInGauntlet = matchNumberInGauntlet,
            participantEntitiesMap = selectedPlayers.toInitialParticipantMap(),
        )
    }

    private fun ArenaMatchEntity.ChoosingClasses.updateChoosingClasses(
        messageGenerator: ChooseCharacterClassMessageGenerator,
        updatedPlayers: Map<String, ArenaPlayerEntity>,
    ): ArenaMatchEntity.ChoosingClasses {
        val updatedMessages = messageGenerator.generateCharacterSelectionMessage()

        return copy(
            playerEntitiesMap = updatedPlayers,
            activePlayerUserId = nextPlayerId,
            newMessages = updatedMessages,
        )
    }

    private inner class ChooseCharacterClassMessageGenerator(
        private val messagesReference: DatabaseReference,
    ) {
        fun generateCharacterSelectionMessage(): List<ArenaMessageEntity> = listOf(element = messagesReference.generateCharacterSelectionMessage())

        fun generateCombatStartingMessages(
            startingCharacterClass: CharacterClassEntity,
        ): List<ArenaMessageEntity> {
            val selectionMessage = messagesReference.generateCharacterSelectionMessage()
            val combatStartingMessages =
                messagesReference.generateInitialCombatRoundMessages(
                    startingCharacterClass = startingCharacterClass,
                )

            return listOf(selectionMessage) + combatStartingMessages
        }

        private fun DatabaseReference.generateCharacterSelectionMessage() = ArenaMessageEntity.ChoseCharacterClass(
            id = addNewKey(),
            characterClassInfo =
            selectedArenaPlayer.characterClassEntity.toCharacterClassMessageInfoEntity(),
            displayName = selectedArenaPlayer.displayName,
        )
    }
}
