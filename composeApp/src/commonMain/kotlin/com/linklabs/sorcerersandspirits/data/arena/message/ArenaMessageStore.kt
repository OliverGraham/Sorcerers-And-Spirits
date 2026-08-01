package com.linklabs.sorcerersandspirits.data.arena.message

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity

data class ArenaMessageUpdate(
    val messages: List<ArenaMessageEntity>,
    val newMessageCount: Int,
)

interface ArenaMessageStore {
    suspend fun appendAndLoad(
        arenaId: String,
        newMessages: List<ArenaMessageEntity>,
    ): ArenaMessageUpdate

    suspend fun clear(arenaId: String)
}
