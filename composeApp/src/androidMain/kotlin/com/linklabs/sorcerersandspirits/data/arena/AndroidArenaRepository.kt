package com.linklabs.sorcerersandspirits.data.arena

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository

class AndroidArenaRepository : ArenaRepository {
    override suspend fun connectToArena(arenaId: String, userId: String) {}
}
