package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import kotlinx.serialization.Serializable

@Serializable
data class ArenaMatchEntity(
    val arenaId: String,
    val phase: ArenaPhaseEntity = ArenaPhaseEntity.WaitingForAllPlayers,
    val activePlayerUserId: String?,
    val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
    val messages: List<ArenaMessageEntity> = emptyList(),
    val numberOfNewMessages: Int = 0,
) {
    companion object {
        val playerEntitiesMapName = ArenaMatchEntity::playerEntitiesMap.name
        val messagesName = ArenaMatchEntity::messages.name
    }
}

internal fun ArenaMatchEntity.toDomain() =
    ArenaMatch(
        phase = phase.toDomain(),
        playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
        activePlayerUserId = activePlayerUserId,
        messages = messages.map { it.toDomain() },
        numberOfNewMessages = numberOfNewMessages,
    )
