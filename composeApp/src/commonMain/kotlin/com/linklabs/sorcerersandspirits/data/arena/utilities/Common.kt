package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import dev.gitlive.firebase.database.DatabaseReference

/**
 * The match node only retains the most recent messages so its payload stays bounded as a match runs
 * long. Older messages scroll off the log; the UI only ever displays recent messages and tracks
 * unhandled ones relative to the tail of the list, so they remain correct after older entries drop.
 */
internal const val MAX_RETAINED_MESSAGES = 30

internal fun List<ArenaMessageEntity>.appendCapped(
    newMessages: List<ArenaMessageEntity>
): List<ArenaMessageEntity> = (this + newMessages).takeLast(n = MAX_RETAINED_MESSAGES)

internal fun DatabaseReference.addNewKey(): String =
    push().key ?: error("Failed to generate Firebase push key")

internal fun Map<String, ArenaPlayerEntity>.getFastestPlayer() =
    values.filterIsInstance<ArenaPlayerEntity.Selected>().maxBy { it.characterClassEntity.speed }

internal fun Map<String, ArenaPlayerEntity>.determineNextChoosingPlayerId(): String {
    val unselected = values.filterIsInstance<ArenaPlayerEntity.Unselected>()

    return if (unselected.isNotEmpty()) {
        unselected.random().userId
    } else {
        getFastestPlayer().userId
    }
}
