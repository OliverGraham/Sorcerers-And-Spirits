package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTime::class)
class UnlockAchievementsUseCaseTest {

    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(NOW_MILLIS)
        }

    private val useCase =
        UnlockAchievementsUseCase(profileRepository = profileRepository, clock = clock)

    @Test
    fun `records newly-satisfied achievements at the clock time`() = runTest {
        coEvery { profileRepository.getProfile(userId = USER_ID) } returns
            Profile(
                userId = USER_ID,
                displayName = "Name",
                stats = ProfileStats(overall = GameStats(matchesWon = 1)),
            )

        useCase(userId = USER_ID)

        coVerify(exactly = 1) {
            profileRepository.unlockAchievements(
                userId = USER_ID,
                achievementIds = listOf(AchievementId.WinAMatch),
                unlockedAt = NOW_MILLIS,
            )
        }
    }

    @Test
    fun `unlocks the per-class achievements once every class has won`() = runTest {
        val perClassMatchWins =
            CharacterClass.playableTypes().associateWith {
                GameStats(matchesWon = 1, gauntletsWon = 1)
            }
        coEvery { profileRepository.getProfile(userId = USER_ID) } returns
            Profile(
                userId = USER_ID,
                displayName = "Name",
                stats =
                    ProfileStats(
                        overall = GameStats(matchesWon = 1, gauntletsWon = 1),
                        perClass = perClassMatchWins,
                    ),
            )

        useCase(userId = USER_ID)

        coVerify(exactly = 1) {
            profileRepository.unlockAchievements(
                userId = USER_ID,
                achievementIds =
                    listOf(
                        AchievementId.WinAMatch,
                        AchievementId.WinAGauntlet,
                        AchievementId.WinAMatchWithEveryClass,
                        AchievementId.WinAGauntletWithEveryClass,
                    ),
                unlockedAt = NOW_MILLIS,
            )
        }
    }

    @Test
    fun `leaves already-unlocked achievements untouched`() = runTest {
        coEvery { profileRepository.getProfile(userId = USER_ID) } returns
            Profile(
                userId = USER_ID,
                displayName = "Name",
                stats = ProfileStats(overall = GameStats(matchesWon = 1)),
                unlockedAchievements = mapOf(AchievementId.WinAMatch to 1L),
            )

        useCase(userId = USER_ID)

        coVerify(exactly = 0) {
            profileRepository.unlockAchievements(
                userId = any(),
                achievementIds = any(),
                unlockedAt = any(),
            )
        }
    }

    @Test
    fun `does nothing when the profile is missing`() = runTest {
        coEvery { profileRepository.getProfile(userId = USER_ID) } returns null

        useCase(userId = USER_ID)

        coVerify(exactly = 0) {
            profileRepository.unlockAchievements(
                userId = any(),
                achievementIds = any(),
                unlockedAt = any(),
            )
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val NOW_MILLIS = 1_700_000_000_000L
    }
}
