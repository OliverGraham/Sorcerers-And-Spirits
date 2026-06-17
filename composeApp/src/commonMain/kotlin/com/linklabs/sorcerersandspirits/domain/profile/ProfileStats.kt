package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

/**
 * The full game-statistics record persisted on a profile. [overall] aggregates every scope, while
 * [perClass] keeps a separate [GameStats] for each class the player has used — enough to derive
 * class-specific milestones (e.g. "win a Gauntlet with every class") without enumerating them here.
 *
 * Like [GameStats], a [ProfileStats] is both the stored totals and a per-write delta.
 */
data class ProfileStats(
    val overall: GameStats = GameStats(),
    val perClass: Map<CharacterClassType, GameStats> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = overall.isEmpty && perClass.values.all { it.isEmpty }
}
