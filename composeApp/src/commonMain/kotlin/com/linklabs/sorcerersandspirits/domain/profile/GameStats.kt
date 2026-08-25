package com.linklabs.sorcerersandspirits.domain.profile

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

    operator fun plus(other: GameStats) = GameStats(
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
