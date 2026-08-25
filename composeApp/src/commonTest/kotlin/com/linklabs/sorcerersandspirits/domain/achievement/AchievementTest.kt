package com.linklabs.sorcerersandspirits.domain.achievement

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementTest {

    @Test
    fun `WinAMatch unlocks on the first match win`() {
        assertFalse(actual = WinAMatch.isUnlockedBy(stats = ProfileStats()))
        assertTrue(
            actual =
            WinAMatch.isUnlockedBy(stats = ProfileStats(overall = GameStats(matchesWon = 1))),
        )
    }

    @Test
    fun `WinAGauntlet unlocks on the first Gauntlet win`() {
        assertFalse(
            actual =
            WinAGauntlet.isUnlockedBy(stats = ProfileStats(overall = GameStats(matchesWon = 5))),
        )
        assertTrue(
            actual =
            WinAGauntlet.isUnlockedBy(
                stats = ProfileStats(overall = GameStats(gauntletsWon = 1)),
            ),
        )
    }

    @Test
    fun `WinAMatchWithEveryClass requires a match win as every playable class`() {
        val missingOne =
            CharacterClass.playableTypes().drop(1).associateWith { GameStats(matchesWon = 1) }
        assertFalse(
            actual =
            WinAMatchWithEveryClass.isUnlockedBy(stats = ProfileStats(perClass = missingOne)),
        )

        val allClasses = CharacterClass.playableTypes().associateWith { GameStats(matchesWon = 1) }
        assertTrue(
            actual =
            WinAMatchWithEveryClass.isUnlockedBy(stats = ProfileStats(perClass = allClasses)),
        )
    }

    @Test
    fun `the unplayable None class does not block every-class achievements`() {
        val playableOnly =
            CharacterClass.playableTypes().associateWith { GameStats(gauntletsWon = 1) }

        assertFalse(actual = playableOnly.containsKey(CharacterClassType.None))
        assertTrue(
            actual =
            WinAGauntletWithEveryClass.isUnlockedBy(
                stats = ProfileStats(perClass = playableOnly),
            ),
        )
    }
}
