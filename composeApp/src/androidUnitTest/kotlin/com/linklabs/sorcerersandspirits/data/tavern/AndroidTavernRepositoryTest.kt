package com.linklabs.sorcerersandspirits.data.tavern

import app.cash.turbine.test
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.data.model.ChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.ChallengerEntity
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.data.model.toDomain
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class AndroidTavernRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val database = mockk<FirebaseDatabase>()
    private val root = mockk<DatabaseReference>()
    private val connectedRef = mockk<DatabaseReference>()
    private val travelersRef = mockk<DatabaseReference>()
    private val mailboxRef = mockk<DatabaseReference>()
    private val challengesRef = mockk<DatabaseReference>()

    private lateinit var repository: AndroidTavernRepository

    @BeforeTest
    fun setUp() {
        every { database.reference } returns root
        every { database.getReference(AndroidTavernRepository.CONNECTED_REFERENCE_PATH) } returns
            connectedRef
        every { root.child(TAVERN_TRAVELERS_PATH) } returns travelersRef
        every { root.child(CHALLENGE_MAILBOXES_PATH) } returns mailboxRef
        every { root.child(CHALLENGES_PATH) } returns challengesRef

        repository = AndroidTavernRepository(database = database)
    }

    @Test
    fun `getTavernTravelersFlow emits mapped travelers`() = runTest {
        val snapshot = mockk<DataSnapshot>()
        val child1 = mockk<DataSnapshot>()
        val child2 = mockk<DataSnapshot>()
        val listenerSlot = slot<ValueEventListener>()

        val travelerEntity1 =
            TravelerEntity(
                userId = "1",
                displayName = "Alice",
                tavernStatus = TavernStatus.Available,
            )
        val travelerEntity2 =
            TravelerEntity(userId = "2", displayName = "Bob", tavernStatus = TavernStatus.Busy)

        every { travelersRef.addValueEventListener(capture(listenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }
        every { travelersRef.removeEventListener(any<ValueEventListener>()) } returns Unit
        every { snapshot.children } returns listOf(child1, child2)
        every { child1.getValue(any<GenericTypeIndicator<TravelerEntity>>()) } returns
            travelerEntity1
        every { child2.getValue(any<GenericTypeIndicator<TravelerEntity>>()) } returns
            travelerEntity2

        repository.getTavernTravelersFlow().test {
            listenerSlot.captured.onDataChange(snapshot)

            assertEquals(
                listOf(
                    Traveler(
                        userId = "1",
                        displayName = "Alice",
                        tavernStatus = TavernStatus.Available,
                    ),
                    Traveler(userId = "2", displayName = "Bob", tavernStatus = TavernStatus.Busy),
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getActiveChallengeFlow emits null when userChallengeMailboxReference is empty`() =
        runTest {
            val userChallengeMailboxReference = mockk<DatabaseReference>()
            val mailboxListenerSlot = slot<ValueEventListener>()
            val emptyMailboxSnapshot = mockk<DataSnapshot>()

            val userId = "user-1"
            every { mailboxRef.child(userId) } returns userChallengeMailboxReference
            every {
                userChallengeMailboxReference.addValueEventListener(capture(mailboxListenerSlot))
            } answers { firstArg<ValueEventListener>() }
            every {
                userChallengeMailboxReference.removeEventListener(any<ValueEventListener>())
            } returns Unit
            every { emptyMailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null

            repository.getActiveChallengeFlow(userId).test {
                mailboxListenerSlot.captured.onDataChange(emptyMailboxSnapshot)

                assertEquals(null, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getActiveChallengeFlow emits a Challenge when userChallengeMailboxReference has a ChallengeId`() =
        runTest {
            val challengeId = "challenge-1"

            val userChallengeMailboxReference = mockk<DatabaseReference>()
            val challengeIdRef = mockk<DatabaseReference>()

            val mailboxListenerSlot = slot<ValueEventListener>()
            val challengeListenerSlot = slot<ValueEventListener>()

            val mailboxSnapshot = mockk<DataSnapshot>()
            val challengeSnapshot = mockk<DataSnapshot>()

            val userId = "user-1"
            val challengeEntity =
                ChallengeEntity(
                    challengeId = challengeId,
                    challengerEntitiesMap =
                        mapOf(
                            userId to
                                ChallengerEntity(
                                    userId = userId,
                                    displayName = "Alice",
                                    isInstigator = true,
                                    status = ChallengerStatus.Accepted,
                                ),
                            "user-2" to
                                ChallengerEntity(
                                    userId = "user-2",
                                    displayName = "Bob",
                                    isInstigator = false,
                                    status = ChallengerStatus.Pending,
                                ),
                        ),
                    startTimeMillis = 1000L,
                    endTimeMillis = 2000L,
                )

            every { mailboxRef.child(userId) } returns userChallengeMailboxReference
            every { challengesRef.child(challengeId) } returns challengeIdRef

            every {
                userChallengeMailboxReference.addValueEventListener(capture(mailboxListenerSlot))
            } answers { firstArg<ValueEventListener>() }
            every {
                userChallengeMailboxReference.removeEventListener(any<ValueEventListener>())
            } returns Unit

            every { challengeIdRef.addValueEventListener(capture(challengeListenerSlot)) } answers
                {
                    firstArg<ValueEventListener>()
                }
            every { challengeIdRef.removeEventListener(any<ValueEventListener>()) } returns Unit

            every { mailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                challengeId
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns challengeEntity

            repository.getActiveChallengeFlow(userId = userId).test {
                mailboxListenerSlot.captured.onDataChange(mailboxSnapshot)
                challengeListenerSlot.captured.onDataChange(challengeSnapshot)

                assertEquals(expected = challengeEntity.toDomain(), actual = awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
}
