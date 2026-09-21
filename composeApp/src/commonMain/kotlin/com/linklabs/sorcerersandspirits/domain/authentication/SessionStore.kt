package com.linklabs.sorcerersandspirits.domain.authentication

interface SessionStore {
    fun cached(): SessionRecord?

    suspend fun restore(): SessionRecord?

    suspend fun save(userId: String, displayName: String)

    suspend fun clear()
}
