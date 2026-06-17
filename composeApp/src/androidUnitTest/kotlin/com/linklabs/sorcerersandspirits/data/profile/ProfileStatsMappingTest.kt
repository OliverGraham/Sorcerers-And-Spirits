package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileStatsMappingTest {

    @Test
    fun `toStatIncrements flattens non-zero overall and per-class counters into nested paths`() {
        val delta =
            ProfileStats(
                overall = GameStats(matchesWon = 1, gauntletsWon = 1, knockoutsDealt = 3),
                perClass =
                    mapOf(CharacterClassType.Mage to GameStats(matchesWon = 1, timesSelected = 1)),
            )

        val increments = delta.toStatIncrements()

        assertEquals(
            expected = 1,
            actual =
                increments[
                    listOf(FireStoreKeys.STATS, FireStoreKeys.OVERALL, FireStoreKeys.MATCHES_WON),
                ],
        )
        assertEquals(
            expected = 3,
            actual =
                increments[
                    listOf(
                        FireStoreKeys.STATS,
                        FireStoreKeys.OVERALL,
                        FireStoreKeys.KNOCKOUTS_DEALT,
                    ),
                ],
        )
        assertEquals(
            expected = 1,
            actual =
                increments[
                    listOf(
                        FireStoreKeys.STATS,
                        FireStoreKeys.PER_CLASS,
                        CharacterClassType.Mage.name,
                        FireStoreKeys.TIMES_SELECTED,
                    ),
                ],
        )
    }

    @Test
    fun `toStatIncrements omits zero-valued counters`() {
        val delta = ProfileStats(overall = GameStats(matchesWon = 1))

        val increments = delta.toStatIncrements()

        assertEquals(expected = 1, actual = increments.size)
        assertTrue(increments.keys.none { it.contains(FireStoreKeys.MATCHES_LOST) })
    }

    @Test
    fun `empty stats produce no increments`() {
        assertTrue(ProfileStats().toStatIncrements().isEmpty())
    }

    @Test
    fun `entity round-trip preserves overall and per-class records`() {
        val stats =
            ProfileStats(
                overall = GameStats(matchesPlayed = 6, matchesWon = 4, gauntletsPlayed = 2),
                perClass =
                    mapOf(
                        CharacterClassType.Rogue to GameStats(matchesWon = 2, knockoutsDealt = 5),
                        CharacterClassType.Bard to GameStats(timesSelected = 1),
                    ),
            )

        assertEquals(expected = stats, actual = stats.toEntity().toDomain())
    }

    @Test
    fun `unknown per-class keys are dropped when mapping to domain`() {
        val entity =
            ProfileStatsEntity(
                perClass =
                    mapOf(
                        CharacterClassType.Mage.name to GameStatsEntity(matchesWon = 1),
                        "RetiredClass" to GameStatsEntity(matchesWon = 9),
                    )
            )

        val domain = entity.toDomain()

        assertEquals(expected = setOf(CharacterClassType.Mage), actual = domain.perClass.keys)
    }
}
