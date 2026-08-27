package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.challengeEntity
import com.linklabs.sorcerersandspirits.data.tavern.challengerEntity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetActiveChallengeFlowUseCaseTest {

    private val dataSource = FakeTavernDataSource()
    private val authenticationRepository: AuthenticationRepository = mockk()

    init {
        every { authenticationRepository.getUserId() } returns USER_ID
    }

    private fun TestScope.useCase() = GetActiveChallengeFlowUseCase(
        authenticationRepository = authenticationRepository,
        tavernRepository =
        FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope),
    )

    @Test
    fun `tags the active challenge with the current user id`() = runTest {
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_A,
                challengers = listOf(challengerEntity(userId = USER_ID)),
            ),
        )
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_A)

        useCase().invoke().test {
            val active = awaitItem()
            assertEquals(expected = USER_ID, actual = active?.userId)
            assertEquals(expected = CHALLENGE_A, actual = active?.challenge?.challengeId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits a null challenge when the mailbox is empty`() = runTest {
        useCase().invoke().test {
            assertNull(actual = awaitItem()?.challenge)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switches to the latest challenge when the mailbox changes`() = runTest {
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_A,
                challengers = listOf(challengerEntity(userId = USER_ID)),
            ),
        )
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_B,
                challengers = listOf(challengerEntity(userId = USER_ID)),
            ),
        )
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_A)

        useCase().invoke().test {
            assertEquals(expected = CHALLENGE_A, actual = awaitItem()?.challenge?.challengeId)

            dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_B)

            assertEquals(expected = CHALLENGE_B, actual = awaitItem()?.challenge?.challengeId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits a null challenge when the mailbox clears`() = runTest {
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_A,
                challengers = listOf(challengerEntity(userId = USER_ID)),
            ),
        )
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_A)

        useCase().invoke().test {
            assertEquals(expected = CHALLENGE_A, actual = awaitItem()?.challenge?.challengeId)

            dataSource.removeMailbox(userId = USER_ID)

            assertNull(actual = awaitItem()?.challenge)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val CHALLENGE_A = "challenge-a"
        const val CHALLENGE_B = "challenge-b"
    }
}
