package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.challengeEntity
import com.linklabs.sorcerersandspirits.data.tavern.challengerEntity
import com.linklabs.sorcerersandspirits.data.tavern.travelerEntity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateChallengerStatusUseCaseTest {

    private val dataSource = FakeTavernDataSource()
    private val authenticationRepository: AuthenticationRepository = mockk()

    private fun TestScope.useCase() = UpdateChallengerStatusUseCase(
        authenticationRepository = authenticationRepository,
        tavernRepository =
        FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope),
    )

    private fun signInAs(userId: String?) {
        every { authenticationRepository.getUserId() } returns userId
    }

    @Test
    fun `does nothing when there is no signed-in user`() = runTest {
        signInAs(userId = null)
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_ID,
                challengers =
                listOf(
                    challengerEntity(userId = INSTIGATOR),
                    challengerEntity(userId = INVITEE),
                ),
            ),
        )

        useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Accepted)

        assertEquals(
            expected = ChallengerStatus.Pending,
            actual =
            dataSource
                .challenge(challengeId = CHALLENGE_ID)
                ?.challengerEntitiesMap
                ?.get(key = INVITEE)
                ?.status,
        )
        assertTrue(actual = dataSource.appliedUpdates.isEmpty())
    }

    @Test
    fun `marks the challenger accepted without cleaning up when not everyone has accepted`() =
        runTest {
            signInAs(userId = INVITEE)
            dataSource.seedChallenge(
                challenge =
                challengeEntity(
                    challengeId = CHALLENGE_ID,
                    challengers =
                    listOf(
                        challengerEntity(
                            userId = INSTIGATOR,
                            status = ChallengerStatus.Accepted,
                        ),
                        challengerEntity(
                            userId = INVITEE,
                            status = ChallengerStatus.Pending,
                        ),
                        challengerEntity(userId = OTHER, status = ChallengerStatus.Pending),
                    ),
                ),
            )

            useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Accepted)

            val challenge = dataSource.challenge(challengeId = CHALLENGE_ID)
            assertEquals(
                expected = ChallengerStatus.Accepted,
                actual = challenge?.challengerEntitiesMap?.get(key = INVITEE)?.status,
            )
            assertTrue(actual = dataSource.appliedUpdates.isEmpty())
        }

    @Test
    fun `completes and clears the challenge when the final challenger accepts`() = runTest {
        signInAs(userId = INVITEE)
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_ID,
                challengers =
                listOf(
                    challengerEntity(
                        userId = INSTIGATOR,
                        status = ChallengerStatus.Accepted,
                    ),
                    challengerEntity(userId = INVITEE, status = ChallengerStatus.Pending),
                ),
            ),
        )
        dataSource.seedMailbox(userId = INSTIGATOR, challengeId = CHALLENGE_ID)
        dataSource.seedMailbox(userId = INVITEE, challengeId = CHALLENGE_ID)

        useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Accepted)

        assertNull(actual = dataSource.challenge(challengeId = CHALLENGE_ID))
        assertNull(actual = dataSource.mailbox(userId = INSTIGATOR))
        assertNull(actual = dataSource.mailbox(userId = INVITEE))
    }

    @Test
    fun `releases the decliner and keeps the challenge when enough challengers remain`() = runTest {
        signInAs(userId = INVITEE)
        dataSource.seedChallenge(
            challenge =
            challengeEntity(
                challengeId = CHALLENGE_ID,
                challengers =
                listOf(
                    challengerEntity(
                        userId = INSTIGATOR,
                        status = ChallengerStatus.Accepted,
                    ),
                    challengerEntity(userId = INVITEE, status = ChallengerStatus.Pending),
                    challengerEntity(userId = OTHER, status = ChallengerStatus.Pending),
                ),
            ),
        )
        dataSource.seedMailbox(userId = INVITEE, challengeId = CHALLENGE_ID)
        dataSource.seedTraveler(
            traveler = travelerEntity(userId = INVITEE, tavernStatus = TavernStatus.Busy),
        )

        useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Declined)

        val challenge = dataSource.challenge(challengeId = CHALLENGE_ID)
        assertEquals(
            expected = ChallengerStatus.Declined,
            actual = challenge?.challengerEntitiesMap?.get(key = INVITEE)?.status,
        )
        assertNull(actual = dataSource.mailbox(userId = INVITEE))
        assertEquals(
            expected = TavernStatus.Available,
            actual = dataSource.traveler(userId = INVITEE)?.tavernStatus,
        )
    }

    @Test
    fun `cancels the challenge and releases travelers when a decline drops below the minimum`() =
        runTest {
            signInAs(userId = INVITEE)
            dataSource.seedChallenge(
                challenge =
                challengeEntity(
                    challengeId = CHALLENGE_ID,
                    challengers =
                    listOf(
                        challengerEntity(
                            userId = INSTIGATOR,
                            status = ChallengerStatus.Accepted,
                        ),
                        challengerEntity(
                            userId = INVITEE,
                            status = ChallengerStatus.Pending,
                        ),
                    ),
                ),
            )
            dataSource.seedMailbox(userId = INSTIGATOR, challengeId = CHALLENGE_ID)
            dataSource.seedTraveler(
                traveler = travelerEntity(userId = INSTIGATOR, tavernStatus = TavernStatus.Busy),
            )

            useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Declined)

            assertNull(actual = dataSource.challenge(challengeId = CHALLENGE_ID))
            assertEquals(
                expected = TavernStatus.Available,
                actual = dataSource.traveler(userId = INSTIGATOR)?.tavernStatus,
            )
        }

    @Test
    fun `does not crash when the challenge no longer exists`() = runTest {
        signInAs(userId = INVITEE)

        useCase().invoke(challengeId = CHALLENGE_ID, status = ChallengerStatus.Accepted)

        assertTrue(actual = dataSource.appliedUpdates.isEmpty())
    }

    private companion object {
        const val CHALLENGE_ID = "challenge-1"
        const val INSTIGATOR = "instigator"
        const val INVITEE = "invitee"
        const val OTHER = "other"
    }
}
