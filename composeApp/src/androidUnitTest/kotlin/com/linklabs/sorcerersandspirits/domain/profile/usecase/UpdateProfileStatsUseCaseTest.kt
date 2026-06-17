package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class UpdateProfileStatsUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk()
    private val profileRepository: ProfileRepository = mockk(relaxed = true)

    private val useCase =
        UpdateProfileStatsUseCase(
            authenticationRepository = authenticationRepository,
            profileRepository = profileRepository,
        )

    @Test
    fun `forwards the delta for the signed-in user`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase(delta = DELTA)

        coVerify(exactly = 1) { profileRepository.incrementStats(userId = USER_ID, delta = DELTA) }
    }

    @Test
    fun `does nothing when no user is signed in`() = runTest {
        every { authenticationRepository.getUserId() } returns null

        useCase(delta = DELTA)

        coVerify(exactly = 0) { profileRepository.incrementStats(userId = any(), delta = any()) }
    }

    @Test
    fun `does nothing when the delta is empty`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase(delta = ProfileStats())

        coVerify(exactly = 0) { profileRepository.incrementStats(userId = any(), delta = any()) }
    }

    private companion object {
        const val USER_ID = "user-1"
        val DELTA = ProfileStats(overall = GameStats(matchesWon = 1))
    }
}
