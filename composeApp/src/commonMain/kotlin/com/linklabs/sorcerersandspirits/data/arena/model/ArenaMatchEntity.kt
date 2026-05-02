package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import dev.gitlive.firebase.FirebaseClassDiscriminator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@FirebaseClassDiscriminator("phase")
sealed interface ArenaMatchEntity {
    val messages: List<ArenaMessageEntity>
    val numberOfNewMessages: Int

    @Serializable
    @SerialName("WaitingForAllPlayers")
    data class WaitingForAllPlayers(
        val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName("ChoosingClasses")
    data class ChoosingClasses(
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
        val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
        val activePlayerUserId: String,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName("Combat")
    data class Combat(
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
        val playerEntitiesMap: Map<String, ArenaPlayerEntity.Selected> = emptyMap(),
        val activePlayerUserId: String,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName("Finished")
    data class Finished(
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
    ) : ArenaMatchEntity

    companion object {
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
