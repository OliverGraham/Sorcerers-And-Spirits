package com.linklabs.sorcerersandspirits.data.arena.message

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO: Just use a list in the view model if messages aren't used outside of the ArenaScreen
class InMemoryArenaMessageStore : ArenaMessageStore {

    private val mutex = Mutex()
    private val logsByArena = mutableMapOf<String, LinkedHashMap<String, ArenaMessageEntity>>()

    override suspend fun appendAndLoad(
        arenaId: String,
        newMessages: List<ArenaMessageEntity>,
        shouldDeleteMessages: Boolean,
    ): ArenaMessageUpdate =
        mutex.withLock {
            if (shouldDeleteMessages) logsByArena.remove(key = arenaId)

            val log = logsByArena.getOrPut(key = arenaId) { LinkedHashMap() }
            val newMessageCount = newMessages.count { message -> message.id !in log }
            newMessages.forEach { message -> log[message.id] = message }

            ArenaMessageUpdate(messages = log.values.toList(), newMessageCount = newMessageCount)
        }

    override suspend fun clear(arenaId: String) {
        mutex.withLock { logsByArena.remove(key = arenaId) }
    }
}
