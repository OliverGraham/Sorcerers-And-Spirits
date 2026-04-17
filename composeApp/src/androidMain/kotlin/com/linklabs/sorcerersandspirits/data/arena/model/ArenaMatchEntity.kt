package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import kotlinx.serialization.Serializable

@Serializable
data class ArenaMatchEntity(
    val arenaId: String = "",
    val phase: ArenaPhase = ArenaPhase.WaitingForAllPlayers,
    val activePlayerUserId: String? = null,
    val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
    val logs: Map<String, ArenaMessageLogEntryEntity> = emptyMap(),
)

internal fun ArenaMatchEntity.toDomain() =
    ArenaMatch(
        phase = phase,
        playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
        activePlayerUserId = activePlayerUserId,
        // TODO: Arena Logs
    )
