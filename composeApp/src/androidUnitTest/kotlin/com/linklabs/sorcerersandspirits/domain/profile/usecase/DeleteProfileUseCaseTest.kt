package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteProfileUseCaseTest {

    private val authenticationRepository: AuthenticationRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val pendingStatsStore: PendingStatsStore = mockk(relaxed = true)

    private val useCase =
        DeleteProfileUseCase(
            authenticationRepository = authenticationRepository,
            profileRepository = profileRepository,
            pendingStatsStore = pendingStatsStore,
        )

    @Test
    fun `deletes the signed-in user's profile`() = runTest {
        every { authenticationRepository.getUserId() } returns USER_ID

        useCase()

        coVerify(exactly = 1) { profileRepository.deleteProfile(userId = USER_ID) }
    }

    @Test
    fun `does nothing when no user is signed in`() = runTest {
        every { authenticationRepository.getUserId() } returns null

        useCase()

        coVerify(exactly = 0) { profileRepository.deleteProfile(userId = any()) }
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
