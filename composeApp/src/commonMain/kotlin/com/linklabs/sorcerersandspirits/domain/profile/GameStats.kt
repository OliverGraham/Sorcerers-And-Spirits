package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet

/**
 * A bundle of cumulative counters for a single scope of play. The same shape is reused for a
 * player's overall record and for each per-class record (see [ProfileStats]), so a counter added
 * here is automatically tracked in every scope.
 *
 * A Gauntlet ([Gauntlet]) is the unit a stats write is attributed to, hence both per-match and
 * per-gauntlet counters.
 *
 * Each [GameStats] doubles as a stored total and as a per-write delta. To add a new stat: add a
 * field here, mirror it on GameStatsEntity, and map it in its toFieldIncrements — [isEmpty] needs
 * no change because it is derived from equality with an all-zero instance.
 */
data class GameStats(
    val matchesPlayed: Long = 0,
    val matchesWon: Long = 0,
    val matchesLost: Long = 0,
    val matchesForfeited: Long = 0,
    val gauntletsPlayed: Long = 0,
    val gauntletsWon: Long = 0,
    val gauntletsLost: Long = 0,
    val gauntletsForfeited: Long = 0,
    val knockoutsDealt: Long = 0,
    val attacksMade: Long = 0,
    val abilitiesUsed: Long = 0,
    val timesSelected: Long = 0,
) {
    val isEmpty: Boolean
        get() = this == GameStats()

    operator fun plus(other: GameStats) =
        GameStats(
            matchesPlayed = matchesPlayed + other.matchesPlayed,
            matchesWon = matchesWon + other.matchesWon,
            matchesLost = matchesLost + other.matchesLost,
            matchesForfeited = matchesForfeited + other.matchesForfeited,
            gauntletsPlayed = gauntletsPlayed + other.gauntletsPlayed,
            gauntletsWon = gauntletsWon + other.gauntletsWon,
            gauntletsLost = gauntletsLost + other.gauntletsLost,
            gauntletsForfeited = gauntletsForfeited + other.gauntletsForfeited,
            knockoutsDealt = knockoutsDealt + other.knockoutsDealt,
            attacksMade = attacksMade + other.attacksMade,
            abilitiesUsed = abilitiesUsed + other.abilitiesUsed,
            timesSelected = timesSelected + other.timesSelected,
        )
}
