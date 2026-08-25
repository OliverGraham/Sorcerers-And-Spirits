package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.data.tavern.FakeTavernDataSource
import com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernRepository
import com.linklabs.sorcerersandspirits.data.tavern.challenger
import com.linklabs.sorcerersandspirits.data.tavern.travelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SendChallengeUseCaseTest {

    private val dataSource = FakeTavernDataSource()

    private fun TestScope.useCase() = SendChallengeUseCase(
        tavernRepository =
        FirebaseTavernRepository(dataSource = dataSource, coroutineScope = backgroundScope),
    )

    @Test
    fun `returns Failure when a challenge id cannot be generated`() = runTest {
        dataSource.challengeIdToGenerate = null

        val result =
            useCase()
                .invoke(
                    instigator = challenger(userId = INSTIGATOR),
                    invitees = listOf(challenger(userId = INVITEE)),
                )

        val failure = assertIs<SendChallengeResult.Failure>(value = result)
        assertEquals(
            expected = FirebaseTavernRepository.SEND_RESULT_FAILURE_MESSAGE,
            actual = failure.errorMessage,
        )
    }

    @Test
    fun `returns Success and writes the challenge with a backend-stamped start time and mailboxes`() = runTest {
        dataSource.serverTimestampMillis = SERVER_NOW
        dataSource.seedTraveler(traveler = travelerEntity(userId = INSTIGATOR))
        dataSource.seedTraveler(traveler = travelerEntity(userId = INVITEE))

        val result =
            useCase()
                .invoke(
                    instigator = challenger(userId = INSTIGATOR, isInstigator = true),
                    invitees = listOf(challenger(userId = INVITEE)),
                )

        val success = assertIs<SendChallengeResult.Success>(value = result)
        assertEquals(
            expected = FakeTavernDataSource.GENERATED_CHALLENGE_ID,
            actual = success.challengeId,
        )

        val challenge = dataSource.challenge(challengeId = CHALLENGE_ID)
        assertEquals(
            expected = setOf(INSTIGATOR, INVITEE),
            actual = challenge?.challengerEntitiesMap?.keys,
        )
        assertEquals(expected = SERVER_NOW, actual = challenge?.startTimeMillis)
        assertEquals(expected = CHALLENGE_ID, actual = dataSource.mailbox(userId = INSTIGATOR))
        assertEquals(expected = CHALLENGE_ID, actual = dataSource.mailbox(userId = INVITEE))
        assertEquals(
            expected = TavernStatus.Busy,
            actual = dataSource.traveler(userId = INSTIGATOR)?.tavernStatus,
        )
        assertEquals(
            expected = TavernStatus.Busy,
            actual = dataSource.traveler(userId = INVITEE)?.tavernStatus,
        )
    }

    @Test
    fun `returns NotEnoughChallengersAvailable and releases the claimed instigator when an invitee is busy`() = runTest {
        dataSource.seedTraveler(traveler = travelerEntity(userId = INSTIGATOR))
        dataSource.seedTraveler(
            traveler = travelerEntity(userId = INVITEE, tavernStatus = TavernStatus.Busy),
        )

        val result =
            useCase()
                .invoke(
                    instigator = challenger(userId = INSTIGATOR, isInstigator = true),
                    invitees = listOf(challenger(userId = INVITEE)),
                )

        val notEnough =
            assertIs<SendChallengeResult.NotEnoughChallengersAvailable>(value = result)
        assertEquals(
            expected = listOf(INVITEE),
            actual = notEnough.unavailableChallengers.map { it.userId },
        )
        assertNull(actual = dataSource.challenge(challengeId = CHALLENGE_ID))
        assertEquals(
            expected = TavernStatus.Available,
            actual = dataSource.traveler(userId = INSTIGATOR)?.tavernStatus,
        )
    }

    @Test
    fun `releases all claimed travelers and returns Failure when the write fails`() = runTest {
        dataSource.seedTraveler(traveler = travelerEntity(userId = INSTIGATOR))
        dataSource.seedTraveler(traveler = travelerEntity(userId = INVITEE))
        dataSource.createChallengeError = RuntimeException("write failed")

        val result =
            useCase()
                .invoke(
                    instigator = challenger(userId = INSTIGATOR, isInstigator = true),
                    invitees = listOf(challenger(userId = INVITEE)),
                )

        assertIs<SendChallengeResult.Failure>(value = result)
        assertEquals(
            expected = TavernStatus.Available,
            actual = dataSource.traveler(userId = INSTIGATOR)?.tavernStatus,
        )
        assertEquals(
            expected = TavernStatus.Available,
            actual = dataSource.traveler(userId = INVITEE)?.tavernStatus,
        )
    }

    @Test
    fun `does not release a claimed traveler whose mailbox already points at a different challenge`() = runTest {
        dataSource.seedTraveler(traveler = travelerEntity(userId = INSTIGATOR))
        dataSource.seedMailbox(userId = INSTIGATOR, challengeId = "other-challenge")
        dataSource.seedTraveler(
            traveler = travelerEntity(userId = INVITEE, tavernStatus = TavernStatus.Busy),
        )

        val result =
            useCase()
                .invoke(
                    instigator = challenger(userId = INSTIGATOR, isInstigator = true),
                    invitees = listOf(challenger(userId = INVITEE)),
                )

        assertIs<SendChallengeResult.NotEnoughChallengersAvailable>(value = result)
        assertEquals(
            expected = TavernStatus.Busy,
            actual = dataSource.traveler(userId = INSTIGATOR)?.tavernStatus,
        )
    }

    private companion object {
        const val INSTIGATOR = "instigator"
        const val INVITEE = "invitee"
        const val CHALLENGE_ID = FakeTavernDataSource.GENERATED_CHALLENGE_ID
        const val SERVER_NOW = 1_000_000L
    }
}
