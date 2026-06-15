package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.challengeEntity
import com.linklabs.sorcerersandspirits.data.tavern.challengerEntity
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectTravelerPresenceUseCaseTest {

    private val dataSource = FakeTavernDataSource()

    // Unconfined so the presence collector launched in
    // [FirebaseTavernRepository.connectTravelerPresence]
    // runs eagerly and reacts to connection changes without manual scheduler advancing.
    private fun runPresenceTest(body: suspend TestScope.() -> Unit) =
        runTest(context = UnconfinedTestDispatcher(), testBody = body)

    private fun TestScope.useCase() =
        ConnectTravelerPresenceUseCase(
            tavernRepository =
                FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope)
        )

    private val profile = Profile(userId = USER_ID, displayName = NAME)

    @Test
    fun `creates an available traveler and registers disconnect when connected with no challenge`() =
        runPresenceTest {
            useCase().invoke(profile = profile)

            dataSource.connected.value = true

            val traveler = dataSource.traveler(userId = USER_ID)
            assertEquals(expected = NAME, actual = traveler?.displayName)
            assertEquals(expected = TavernStatus.Available, actual = traveler?.tavernStatus)
            assertTrue(actual = dataSource.disconnectRegistrations.contains(element = USER_ID))
        }

    @Test
    fun `does not touch presence while disconnected`() = runPresenceTest {
        useCase().invoke(profile = profile)

        assertNull(actual = dataSource.traveler(userId = USER_ID))
        assertTrue(actual = dataSource.disconnectRegistrations.isEmpty())
    }

    @Test
    fun `marks the traveler busy when an active non-expired challenge exists`() = runPresenceTest {
        dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_ID)
        dataSource.seedChallenge(
            challenge =
                challengeEntity(
                    challengeId = CHALLENGE_ID,
                    challengers = listOf(challengerEntity(userId = USER_ID)),
                    startTimeMillis = NON_EXPIRED_START,
                )
        )

        useCase().invoke(profile = profile)
        dataSource.connected.value = true

        assertEquals(
            expected = TavernStatus.Busy,
            actual = dataSource.traveler(userId = USER_ID)?.tavernStatus,
        )
    }

    @Test
    fun `clears the mailbox and stays available when the referenced challenge is missing`() =
        runPresenceTest {
            dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_ID)

            useCase().invoke(profile = profile)
            dataSource.connected.value = true

            assertNull(actual = dataSource.mailbox(userId = USER_ID))
            assertEquals(
                expected = TavernStatus.Available,
                actual = dataSource.traveler(userId = USER_ID)?.tavernStatus,
            )
        }

    @Test
    fun `expires the challenge and stays available when the challenge has ended`() =
        runPresenceTest {
            dataSource.seedMailbox(userId = USER_ID, challengeId = CHALLENGE_ID)
            dataSource.seedChallenge(
                challenge =
                    challengeEntity(
                        challengeId = CHALLENGE_ID,
                        challengers = listOf(challengerEntity(userId = USER_ID)),
                        startTimeMillis = 1L,
                    )
            )

            useCase().invoke(profile = profile)
            dataSource.connected.value = true

            assertNull(actual = dataSource.challenge(challengeId = CHALLENGE_ID))
            assertNull(actual = dataSource.mailbox(userId = USER_ID))
            assertEquals(
                expected = TavernStatus.Available,
                actual = dataSource.traveler(userId = USER_ID)?.tavernStatus,
            )
        }

    private companion object {
        const val USER_ID = "user-1"
        const val NAME = "Gandalf"
        const val CHALLENGE_ID = "challenge-1"

        // Far enough out that start + 60s is never in the past during the test run.
        const val NON_EXPIRED_START = 4_000_000_000_000L
    }
}
