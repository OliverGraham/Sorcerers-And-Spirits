package com.linklabs.sorcerersandspirits.domain.profile

interface PendingStatsStore {
    suspend fun load(): ProfileStats

    suspend fun save(delta: ProfileStats)

    suspend fun clear()
}
