package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType

data class ProfileStats(
    val overall: GameStats = GameStats(),
    val perClass: Map<CharacterClassType, GameStats> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = overall.isEmpty && perClass.values.all { it.isEmpty }

    val rating: Long
        get() {
            val earned =
                GAUNTLET_WON_POINTS * overall.gauntletsWon +
                    MATCH_WON_POINTS * overall.matchesWon +
                    KNOCKOUT_POINTS * overall.knockoutsDealt
            val forfeits = overall.matchesForfeited + overall.gauntletsForfeited
            val penalties =
                GAUNTLET_LOST_PENALTY * overall.gauntletsLost +
                    MATCH_LOST_PENALTY * overall.matchesLost +
                    FORFEIT_PENALTY * forfeits
            return (earned - penalties).coerceAtLeast(minimumValue = 0)
        }

    fun hasMatchWinAs(classType: CharacterClassType): Boolean = (perClass[classType]?.matchesWon ?: 0) >= 1

    fun hasGauntletWinAs(classType: CharacterClassType): Boolean = (perClass[classType]?.gauntletsWon ?: 0) >= 1

    operator fun plus(other: ProfileStats): ProfileStats {
        val classes = perClass.keys + other.perClass.keys
        return ProfileStats(
            overall = overall + other.overall,
            perClass =
            classes.associateWith { classType ->
                (perClass[classType] ?: GameStats()) +
                    (other.perClass[classType] ?: GameStats())
            },
        )
    }

    private companion object {
        const val GAUNTLET_WON_POINTS = 100L
        const val MATCH_WON_POINTS = 25L
        const val KNOCKOUT_POINTS = 2L
        const val GAUNTLET_LOST_PENALTY = 15L
        const val MATCH_LOST_PENALTY = 5L
        const val FORFEIT_PENALTY = 20L
    }
}
