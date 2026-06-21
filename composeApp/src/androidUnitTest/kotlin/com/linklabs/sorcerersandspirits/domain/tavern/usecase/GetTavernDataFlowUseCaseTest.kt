package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.travelerEntity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GetTavernDataFlowUseCaseTest {

    private val dataSource = FakeTavernDataSource()
    private val authenticationRepository: AuthenticationRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val profileFlow =
        MutableStateFlow<Profile?>(value = Profile(userId = USER_ID, displayName = NAME))

    init {
        every { authenticationRepository.getUserId() } returns USER_ID
        every { profileRepository.getProfileFlow(userId = USER_ID) } returns profileFlow
    }

    private fun TestScope.useCase() =
        GetTavernDataFlowUseCase(
            authenticationRepository = authenticationRepository,
            profileRepository = profileRepository,
            tavernRepository =
                FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope),
        )

    @Test
    fun `emits mapped travelers`() = runTest {
        dataSource.seedTraveler(traveler = travelerEntity(userId = "alice", displayName = "Alice"))

        useCase().invoke().test {
            val data = awaitItem()
            assertEquals(
                expected = listOf("Alice"),
                actual = data?.travelers?.map { it.displayName },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val NAME = "Gandalf"
    }
}
