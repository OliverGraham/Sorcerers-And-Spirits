package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.travelerEntity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SetCurrentTravelerAvailableUseCaseTest {

    private val dataSource = FakeTavernDataSource()
    private val authenticationRepository: AuthenticationRepository = mockk()

    private fun TestScope.useCase() =
        SetCurrentTravelerAvailableUseCase(
            authenticationRepository = authenticationRepository,
            tavernRepository =
                FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope),
        )

    private fun signInAs(userId: String?) {
        every { authenticationRepository.getUserId() } returns userId
    }

    @Test
    fun `sets the traveler to available when present`() = runTest {
        signInAs(userId = USER_ID)
        dataSource.seedTraveler(
            traveler = travelerEntity(userId = USER_ID, tavernStatus = TavernStatus.Busy)
        )

        useCase().invoke()

        assertEquals(
            expected = TavernStatus.Available,
            actual = dataSource.traveler(userId = USER_ID)?.tavernStatus,
        )
    }

    @Test
    fun `is a no-op when the traveler does not exist`() = runTest {
        signInAs(userId = USER_ID)

        useCase().invoke()

        assertNull(actual = dataSource.traveler(userId = USER_ID))
    }

    @Test
    fun `does nothing when there is no signed-in user`() = runTest {
        signInAs(userId = null)
        dataSource.seedTraveler(
            traveler = travelerEntity(userId = USER_ID, tavernStatus = TavernStatus.Busy)
        )

        useCase().invoke()

        assertEquals(
            expected = TavernStatus.Busy,
            actual = dataSource.traveler(userId = USER_ID)?.tavernStatus,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
