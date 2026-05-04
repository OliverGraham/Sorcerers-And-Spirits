package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity.Companion.PHASE
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import dev.gitlive.firebase.FirebaseClassDiscriminator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@FirebaseClassDiscriminator(discriminator = PHASE)
sealed interface ArenaMatchEntity {
    val messages: List<ArenaMessageEntity>
    val numberOfNewMessages: Int

    @Serializable
    @SerialName(value = WAITING_FOR_ALL_PLAYERS)
    data class WaitingForAllPlayers(
        val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        override val messages: List<ArenaMessageEntity>,
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

        is ArenaMatchEntity.ChoosingClasses ->
            ArenaMatch.ChoosingClasses(
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                activePlayerUserId = activePlayerUserId,
            )

        is ArenaMatchEntity.Combat ->
            ArenaMatch.Combat(
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                activePlayerUserId = activePlayerUserId,
            )

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(messages = messages, numberOfNewMessages = numberOfNewMessages)
    }
}
