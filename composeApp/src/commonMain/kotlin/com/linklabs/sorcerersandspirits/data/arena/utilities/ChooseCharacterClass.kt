package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageType
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import dev.gitlive.firebase.database.DatabaseReference

class ChooseCharacterClass(
    private val arenaReference: DatabaseReference,
    private val selectedArenaPlayer: ArenaPlayer.Selected,
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

    private fun ArenaMatchEntity.chooseCharacterClassTransaction() =
        when (this) {
            is ArenaMatchEntity.ChoosingClasses -> chooseCharacterClassTransaction()

            is ArenaMatchEntity.WaitingForAllPlayers,
            is ArenaMatchEntity.Combat,
            is ArenaMatchEntity.Finished -> this
        }

    private fun ArenaMatchEntity.ChoosingClasses.chooseCharacterClassTransaction():
        ArenaMatchEntity {
        val messageGenerator =
            ChooseCharacterClassMessageGenerator(
                messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName),
                previousMessages = messages,
            )

        val updatedPlayers = playerEntitiesMap.toMutableMap()
        updatedPlayers[selectedArenaPlayer.userId] = selectedArenaPlayer.toEntity()

        val areAllSelected = updatedPlayers.values.all { it is ArenaPlayerEntity.Selected }

        return if (areAllSelected) {
            transitionToCombat(
                messageGenerator = messageGenerator,
                selectedPlayers =
                    updatedPlayers.values.associate {
                        it.userId to it as ArenaPlayerEntity.Selected
                    },
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
    ): ArenaMatchEntity.Combat {
        val (updatedMessages, numberOfNewMessages) =
            messageGenerator.generateCombatStartingMessages(
                startingCharacterClass =
                    selectedPlayers.values.maxBy { it.characterClassEntity.speed }
            )

        return ArenaMatchEntity.Combat(
            playerEntitiesMap = selectedPlayers,
            activePlayerUserId = nextPlayerId,
            messages = updatedMessages,
            numberOfNewMessages = numberOfNewMessages,
        )
    }

    private fun ArenaMatchEntity.ChoosingClasses.updateChoosingClasses(
        messageGenerator: ChooseCharacterClassMessageGenerator,
        updatedPlayers: Map<String, ArenaPlayerEntity>,
    ): ArenaMatchEntity.ChoosingClasses {
        val (updatedMessages, numberOfNewMessages) =
            messageGenerator.generateCharacterSelectionMessage()

        return copy(
            playerEntitiesMap = updatedPlayers,
            activePlayerUserId = nextPlayerId,
            messages = updatedMessages,
            numberOfNewMessages = numberOfNewMessages,
        )
    }

    private inner class ChooseCharacterClassMessageGenerator(
        private val messagesReference: DatabaseReference,
        private val previousMessages: List<ArenaMessageEntity>,
    ) {
        fun generateCharacterSelectionMessage(): Pair<List<ArenaMessageEntity>, Int> {
            val selectionMessage =
                listOf(element = messagesReference.generateCharacterSelectionMessage())

            return Pair(first = previousMessages + selectionMessage, second = selectionMessage.size)
        }

        fun generateCombatStartingMessages(
            startingCharacterClass: ArenaPlayerEntity.Selected
        ): Pair<List<ArenaMessageEntity>, Int> {
            val selectionMessage = messagesReference.generateCharacterSelectionMessage()
            val combatStartingMessages =
                messagesReference.generateInitialCombatRoundMessages(
                    startingCharacterClass = startingCharacterClass
                )

            val newMessages = listOf(selectionMessage) + combatStartingMessages
            return Pair(first = previousMessages + newMessages, second = newMessages.size)
        }

        private fun DatabaseReference.generateCharacterSelectionMessage() =
            ArenaMessageEntity(
                id = addNewKey(),
                type = ArenaMessageType.Generic,
                message =
                    "${selectedArenaPlayer.displayName} selected ${selectedArenaPlayer.characterClass.className}",
            )

        private fun DatabaseReference.generateInitialCombatRoundMessages(
            startingCharacterClass: ArenaPlayerEntity.Selected
        ) = buildList {
            add(
                element =
                    ArenaMessageEntity(
                        id = addNewKey(),
                        type = ArenaMessageType.Generic,
                        message = "Combat is now starting!",
                    )
            )
            add(
                element =
                    ArenaMessageEntity(
                        id = addNewKey(),
                        type = ArenaMessageType.Round,
                        roundNumber = 1,
                    )
            )

            add(
                element =
                    ArenaMessageEntity(
                        id = addNewKey(),
                        type = ArenaMessageType.Generic,
                        message =
                            "It is now ${startingCharacterClass.characterClassEntity.className}'s turn!",
                    )
            )
        }

        private fun DatabaseReference.addNewKey(): String =
            push().key ?: error("Failed to generate Firebase push key")
    }
}
