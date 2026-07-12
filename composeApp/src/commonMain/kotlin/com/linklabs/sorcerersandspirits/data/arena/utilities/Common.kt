package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassMessageInfoEntity
import dev.gitlive.firebase.database.DatabaseReference

internal const val MAX_RETAINED_MESSAGES = 30

internal fun List<ArenaMessageEntity>.appendCapped(
    newMessages: List<ArenaMessageEntity>
): List<ArenaMessageEntity> = (this + newMessages).takeLast(n = MAX_RETAINED_MESSAGES)

internal fun DatabaseReference.addNewKey(): String =
    push().key ?: error("Failed to generate Firebase push key")

internal fun Map<String, ArenaPlayerEntity>.getFastestPlayer() =
    values.filterIsInstance<ArenaPlayerEntity.Selected>().maxBy { it.characterClassEntity.speed }

internal fun CharacterClassEntity.toCharacterClassMessageInfoEntity() =
    CharacterClassMessageInfoEntity(className = className, type = type)

/**
 * The opening log entries for a fresh combat match: round 1 begins, then the first player's turn.
 */
internal fun DatabaseReference.generateInitialCombatRoundMessages(
    startingCharacterClass: CharacterClassEntity
): List<ArenaMessageEntity> =
    listOf(
        ArenaMessageEntity.NewRound(id = addNewKey(), roundNumber = 1),
        ArenaMessageEntity.NewTurn(
            id = addNewKey(),
            characterClassInfo = startingCharacterClass.toCharacterClassMessageInfoEntity(),
        ),
    )

internal fun Map<String, ArenaPlayerEntity>.determineNextChoosingPlayerId(): String {
    val unselected = values.filterIsInstance<ArenaPlayerEntity.Unselected>()

    return if (unselected.isNotEmpty()) {
        unselected.random().userId
    } else {
        getFastestPlayer().userId
    }
}
