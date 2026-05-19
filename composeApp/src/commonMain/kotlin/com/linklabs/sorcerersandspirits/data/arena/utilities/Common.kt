package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import dev.gitlive.firebase.database.DatabaseReference

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
