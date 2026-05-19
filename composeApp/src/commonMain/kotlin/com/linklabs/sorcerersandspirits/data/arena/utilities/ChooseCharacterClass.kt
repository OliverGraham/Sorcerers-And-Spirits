package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassMessageInfoEntity
import dev.gitlive.firebase.database.DatabaseReference

class ChooseCharacterClass(
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
        updatedPlayers[selectedArenaPlayer.userId] = selectedArenaPlayer

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
                startingCharacterClass = selectedPlayers.getFastestPlayer().characterClassEntity
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
            startingCharacterClass: CharacterClassEntity
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
            ArenaMessageEntity.ChoseCharacterClass(
                id = addNewKey(),
                characterClassInfo =
                    selectedArenaPlayer.characterClassEntity.toCharacterClassMessageInfoEntity(),
                displayName = selectedArenaPlayer.displayName,
            )

        private fun DatabaseReference.generateInitialCombatRoundMessages(
            startingCharacterClass: CharacterClassEntity
        ) = buildList {
            add(element = ArenaMessageEntity.NewRound(id = addNewKey(), roundNumber = 1))
            add(
                element =
                    ArenaMessageEntity.NewTurn(
                        id = addNewKey(),
                        characterClassInfo =
                            startingCharacterClass.toCharacterClassMessageInfoEntity(),
                    )
            )
        }

        private fun CharacterClassEntity.toCharacterClassMessageInfoEntity() =
            CharacterClassMessageInfoEntity(className = className, type = type)
    }
}
