package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class FlushPendingStatsUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val pendingStatsStore = FakePendingStatsStore()

    private val useCase =
        FlushPendingStatsUseCase(
            pendingStatsStore = pendingStatsStore,
            updateProfileStatsUseCase =
            UpdateProfileStatsUseCase(
                authenticationRepository = authenticationRepository,
                profileRepository = profileRepository,
                unlockAchievementsUseCase = mockk(relaxed = true),
            ),
        )

    @Test
    fun `replays the pending delta then clears it once written`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID
        pendingStatsStore.stored = DELTA

        useCase()

        coVerify(exactly = 1) { profileRepository.incrementStats(userId = USER_ID, delta = DELTA) }
        assertNull(actual = pendingStatsStore.stored)
    }

    @Test
    fun `keeps the pending delta when no user is signed in`() = runTest {
        every { authenticationRepository.getUserId() } returns null
        pendingStatsStore.stored = DELTA

        useCase()

        coVerify(exactly = 0) { profileRepository.incrementStats(userId = any(), delta = any()) }
        assert(pendingStatsStore.stored == DELTA)
    }

    @Test
    fun `does nothing when there is no pending delta`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase()

        coVerify(exactly = 0) { profileRepository.incrementStats(userId = any(), delta = any()) }
    }

    private class FakePendingStatsStore : PendingStatsStore {
        var stored: ProfileStats? = null

        override suspend fun load(): ProfileStats = stored ?: ProfileStats()

        override suspend fun save(delta: ProfileStats) {
            stored = delta
        }

        override suspend fun clear() {
            stored = null
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        val DELTA = ProfileStats(overall = GameStats(matchesWon = 1))
    }
}
