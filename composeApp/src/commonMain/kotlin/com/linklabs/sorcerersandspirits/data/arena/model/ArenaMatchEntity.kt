package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
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
        val pendingRollType: com.linklabs.sorcerersandspirits.domain.arena.model.RollType =
            com.linklabs.sorcerersandspirits.domain.arena.model.RollType.ArmorClass,
        val pendingTargetUserId: String? = null,
        val turnOrder: List<String> = emptyList(),
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = FINISHED)
    data class Finished(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
    ) : ArenaMatchEntity

    companion object {
        private const val PHASE = "phase"
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
                    turnOrder = turnOrder,
                )
            }
        }

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(messages = messages, numberOfNewMessages = numberOfNewMessages)
    }
}
