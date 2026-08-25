package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileStatsRatingTest {

    @Test
    fun `an empty record rates zero`() {
        assertEquals(expected = 0L, actual = ProfileStats().rating)
    }

    @Test
    fun `weights wins and knockouts against losses and forfeits`() {
        val stats =
            ProfileStats(
                overall =
                GameStats(
                    gauntletsWon = 2, // 200
                    matchesWon = 5, // 125
                    knockoutsDealt = 10, // 20
                    gauntletsLost = 1, // -15
                    matchesLost = 3, // -15
                    matchesForfeited = 1, // -20
                    gauntletsForfeited = 0,
                ),
            )

        assertEquals(expected = 295L, actual = stats.rating)
    }

    @Test
    fun `never goes negative`() {
        val stats = ProfileStats(overall = GameStats(matchesLost = 50, gauntletsForfeited = 10))

        assertEquals(expected = 0L, actual = stats.rating)
    }

    @Test
    fun `is derived from the overall scope only, not per-class`() {
        val stats =
            ProfileStats(
                overall = GameStats(matchesWon = 4),
                perClass = mapOf(CharacterClassType.Mage to GameStats(matchesWon = 99)),
            )

        assertEquals(expected = 100L, actual = stats.rating)
    }
}
