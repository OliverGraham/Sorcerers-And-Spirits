package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
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
    fun `forwards increments for the signed-in user`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase(increments = INCREMENTS)

        coVerify(exactly = 1) {
            profileRepository.incrementStats(userId = USER_ID, increments = INCREMENTS)
        }
    }

    @Test
    fun `does nothing when no user is signed in`() = runTest {
        every { authenticationRepository.getUserId() } returns null

        useCase(increments = INCREMENTS)

        coVerify(exactly = 0) {
            profileRepository.incrementStats(userId = any(), increments = any())
        }
    }

    @Test
    fun `does nothing when there are no increments`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase(increments = emptyMap())

        coVerify(exactly = 0) {
            profileRepository.incrementStats(userId = any(), increments = any())
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        val INCREMENTS = mapOf("matchesPlayed" to 1, "wins" to 1)
    }
}
