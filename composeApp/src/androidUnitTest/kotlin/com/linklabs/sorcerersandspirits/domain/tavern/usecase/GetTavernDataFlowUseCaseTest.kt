package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import app.cash.turbine.test
import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.challengeEntity
import com.linklabs.sorcerersandspirits.data.tavern.challengerEntity
import com.linklabs.sorcerersandspirits.data.tavern.travelerEntity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun `emits mapped travelers and a null challenge when the mailbox is empty`() = runTest {
        dataSource.seedTraveler(traveler = travelerEntity(userId = "alice", displayName = "Alice"))

        useCase().invoke().test {
            val data = awaitItem()
            assertEquals(
                expected = listOf("Alice"),
                actual = data?.travelers?.map { it.displayName },
            )
            assertNull(actual = data?.challenge)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits the challenge referenced by the mailbox`() = runTest {
        dataSource.seedChallenge(
            challenge =
                challengeEntity(
                    challengeId = CHALLENGE_A,
                    challengers = listOf(challengerEntity(userId = USER_ID)),
                )
        )
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_A)

        useCase().invoke().test {
            assertEquals(expected = CHALLENGE_A, actual = awaitItem()?.challenge?.challengeId)
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
                )
        )
        dataSource.seedChallenge(
            challenge =
                challengeEntity(
                    challengeId = CHALLENGE_B,
                    challengers = listOf(challengerEntity(userId = USER_ID)),
                )
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
    fun `emits a null challenge when the referenced challenge entity disappears`() = runTest {
        dataSource.seedChallenge(
            challenge =
                challengeEntity(
                    challengeId = CHALLENGE_A,
                    challengers = listOf(challengerEntity(userId = USER_ID)),
                )
        )
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_A)

        useCase().invoke().test {
            assertEquals(expected = CHALLENGE_A, actual = awaitItem()?.challenge?.challengeId)

            dataSource.challenges.value = emptyMap()

            assertNull(actual = awaitItem()?.challenge)
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
                )
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
        const val NAME = "Gandalf"
        const val CHALLENGE_A = "challenge-a"
        const val CHALLENGE_B = "challenge-b"
    }
}
