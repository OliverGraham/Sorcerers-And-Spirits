package com.linklabs.sorcerersandspirits.domain.arena

interface ArenaRepository {
    suspend fun connectToArena(arenaId: String, userId: String)
}
