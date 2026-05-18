package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMatchEntity {
    val messages: List<ArenaMessageEntity>
    val numberOfNewMessages: Int

    @Serializable
    @SerialName(value = WAITING_FOR_ALL_PLAYERS)
    data class WaitingForAllPlayers(
        val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = CHOOSING_CLASSES)
    data class ChoosingClasses(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        val activePlayerUserId: String,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = COMBAT)
    data class Combat(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        val playerEntitiesMap: Map<String, ArenaPlayerEntity.Selected>,
        val activePlayerUserId: String,
        val pendingRollType: RollType = RollType.ArmorClass,
        val pendingTargetUserId: String? = null,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = FINISHED)
    data class Finished(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatchEntity

    companion object {
        private const val WAITING_FOR_ALL_PLAYERS = "WaitingForAllPlayers"
        private const val CHOOSING_CLASSES = "ChoosingClasses"
        private const val COMBAT = "Combat"
        private const val FINISHED = "Finished"

        val playerEntitiesMapName = ChoosingClasses::playerEntitiesMap.name
        val messagesName = ArenaMatchEntity::messages.name
    }
}

internal fun ArenaMatchEntity.toDomain(): ArenaMatch {
    val messages = messages.map { it.toDomain() }

    return when (this) {
        is ArenaMatchEntity.WaitingForAllPlayers ->
            ArenaMatch.WaitingForAllPlayers(
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
            )

        is ArenaMatchEntity.ChoosingClasses -> {
            val playerMap = playerEntitiesMap.mapValues { it.value.toDomain() }

            val activePlayer = playerMap[activePlayerUserId]
            if (activePlayer == null) {
                ArenaMatch.Finished(messages = messages, numberOfNewMessages = numberOfNewMessages)
            } else {
                ArenaMatch.ChoosingClasses(
                    playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                    messages = messages,
                    numberOfNewMessages = numberOfNewMessages,
                    activePlayerUserId = activePlayerUserId,
                    activePlayer = activePlayer,
                )
            }
        }

        is ArenaMatchEntity.Combat -> {
            val playerMap = playerEntitiesMap.mapValues { it.value.toDomain() }

            val activePlayer = playerMap[activePlayerUserId]
            if (activePlayer == null) {
                ArenaMatch.Finished(messages = messages, numberOfNewMessages = numberOfNewMessages)
            } else {
                ArenaMatch.Combat(
                    playerMap = playerMap,
                    messages = messages,
                    numberOfNewMessages = numberOfNewMessages,
                    activePlayerUserId = activePlayerUserId,
                    activePlayer = activePlayer,
                    pendingRollType = pendingRollType,
                    pendingTargetUserId = pendingTargetUserId,
                )
            }
        }

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(messages = messages, numberOfNewMessages = numberOfNewMessages)
    }
}

internal fun ArenaMatch.toEntity(
    newMessages: List<OutgoingArenaMessage>,
    addNewMessageKey: () -> String,
): ArenaMatchEntity {
    val existingMessages = messages.map { it.toEntity() }
    val mappedNewMessages = newMessages.map { it.toEntity(id = addNewMessageKey()) }
    val allMessages = existingMessages + mappedNewMessages

    return when (this) {
        is ArenaMatch.WaitingForAllPlayers ->
            ArenaMatchEntity.WaitingForAllPlayers(
                messages = allMessages,
                numberOfNewMessages = mappedNewMessages.size,
                playerEntitiesMap = emptyMap(), // TODO: Update if needed
            )

        is ArenaMatch.ChoosingClasses ->
            ArenaMatchEntity.ChoosingClasses(
                messages = allMessages,
                numberOfNewMessages = mappedNewMessages.size,
                playerEntitiesMap = playerMap.mapValues { it.value.toEntity() },
                activePlayerUserId = activePlayerUserId,
            )

        is ArenaMatch.Combat ->
            ArenaMatchEntity.Combat(
                messages = allMessages,
                numberOfNewMessages = mappedNewMessages.size,
                playerEntitiesMap =
                    playerMap.mapValues { it.value.toEntity() as ArenaPlayerEntity.Selected },
                activePlayerUserId = activePlayerUserId,
                pendingRollType = pendingRollType,
                pendingTargetUserId = pendingTargetUserId,
            )

        is ArenaMatch.Finished ->
            ArenaMatchEntity.Finished(
                messages = allMessages,
                numberOfNewMessages = mappedNewMessages.size,
            )
    }
}
