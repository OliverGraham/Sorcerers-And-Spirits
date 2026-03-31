package com.linklabs.sorcerersandspirits.data.tavern

import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.OnDisconnect
import com.google.firebase.database.Transaction
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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidTavernRepositoryTest {

    private val testScope = TestScope(context = StandardTestDispatcher())

    private val database = mockk<FirebaseDatabase>()
    private val root = mockk<DatabaseReference>()
    private val connectedRef = mockk<DatabaseReference>()
    private val travelersRef = mockk<DatabaseReference>()
    private val mailboxRef = mockk<DatabaseReference>()
    private val challengesRef = mockk<DatabaseReference>()

    private val travelerReference = mockk<DatabaseReference>()
    private val userChallengeMailboxReference = mockk<DatabaseReference>()
    private val onDisconnect = mockk<OnDisconnect>()
    private val connectedSnapshot = mockk<DataSnapshot>()
    private val mailboxSnapshot = mockk<DataSnapshot>()
    private val connectedListenerSlot = slot<ValueEventListener>()
    private val transactionHandlerSlot = slot<Transaction.Handler>()
    private val mutableData = mockk<MutableData>()
    private val challengeReference = mockk<DatabaseReference>()
    private val challengeSnapshot = mockk<DataSnapshot>()

    private lateinit var repository: AndroidTavernRepository

    @BeforeTest
    fun setUp() {
        every { database.reference } returns root
        every { database.getReference(AndroidTavernRepository.CONNECTED_REFERENCE_PATH) } returns
            connectedRef
        every { root.child(TAVERN_TRAVELERS_PATH) } returns travelersRef
        every { root.child(CHALLENGE_MAILBOXES_PATH) } returns mailboxRef
        every { root.child(CHALLENGES_PATH) } returns challengesRef

        repository = AndroidTavernRepository(database = database, coroutineScope = testScope)
    }

    @Test
    fun `getTavernTravelersFlow emits mapped travelers`() = runTest {
        val snapshot = mockk<DataSnapshot>()
        val child1 = mockk<DataSnapshot>()
        val child2 = mockk<DataSnapshot>()
        val listenerSlot = slot<ValueEventListener>()

        val travelerEntity1 =
            TravelerEntity(
                userId = USER_ID_1,
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
                        userId = USER_ID_1,
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

            every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
            every {
                userChallengeMailboxReference.addValueEventListener(capture(mailboxListenerSlot))
            } answers { firstArg<ValueEventListener>() }
            every {
                userChallengeMailboxReference.removeEventListener(any<ValueEventListener>())
            } returns Unit
            every { emptyMailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null

            repository.getActiveChallengeFlow(userId = USER_ID_1).test {
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

            val challengeEntity =
                ChallengeEntity(
                    challengeId = challengeId,
                    challengerEntitiesMap =
                        mapOf(
                            USER_ID_1 to
                                ChallengerEntity(
                                    userId = USER_ID_1,
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

            every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
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

            repository.getActiveChallengeFlow(userId = USER_ID_1).test {
                mailboxListenerSlot.captured.onDataChange(mailboxSnapshot)
                challengeListenerSlot.captured.onDataChange(challengeSnapshot)

                assertEquals(expected = challengeEntity.toDomain(), actual = awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `connectTravelerPresence adds value event listener to connected reference`() = runTest {
        val listenerSlot = slot<ValueEventListener>()

        every { connectedRef.addValueEventListener(capture(listenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }

        repository.connectTravelerPresence(userId = USER_ID_1, displayName = "Alice")

        verify(exactly = 1) { connectedRef.addValueEventListener(any()) }
    }

    @Test
    fun `ConnectedValueEventListener calls onConnectToDatabase when connected becomes true`() =
        runTest {
            val snapshot = mockk<DataSnapshot>()
            var wasCalled = false

            every { snapshot.getValue(any<GenericTypeIndicator<Boolean>>()) } returns true

            val listener =
                ConnectedValueEventListener(
                    coroutineScope = backgroundScope,
                    onConnectToDatabase = { wasCalled = true },
                )

            listener.onDataChange(snapshot)
            testScheduler.runCurrent()

            assertEquals(expected = true, actual = wasCalled)
        }

    @Test
    fun `ConnectedValueEventListener does not call onConnectToDatabase when connected becomes false`() =
        runTest {
            val snapshot = mockk<DataSnapshot>()
            var wasCalled = false

            every { snapshot.getValue(any<GenericTypeIndicator<Boolean>>()) } returns false

            val listener =
                ConnectedValueEventListener(
                    coroutineScope = backgroundScope,
                    onConnectToDatabase = { wasCalled = true },
                )

            listener.onDataChange(snapshot)
            testScheduler.runCurrent()

            assertEquals(expected = false, actual = wasCalled)
        }

    @Test
    fun `connectTravelerPresence sets traveler available when mailbox challenge id is null or blank`() =
        runTest {
            setUpConnectTravelerPresence(mailboxChallengeId = null)

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
            verify(exactly = 0) { challengesRef.child(any()) }
        }

    @Test
    fun `connectTravelerPresence clears mailbox and sets traveler available when challenge entity is null`() =
        runTest {
            setUpConnectTravelerPresence(mailboxChallengeId = CHALLENGE_ID_1)

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns null
            every { userChallengeMailboxReference.removeValue() } returns Tasks.forResult(null)

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { challengesRef.child(CHALLENGE_ID_1) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 1) { userChallengeMailboxReference.removeValue() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
        }

    @Test
    fun `connectTravelerPresence expires challenge and sets traveler available when challenge is expired`() =
        runTest {
            setUpConnectTravelerPresence(mailboxChallengeId = CHALLENGE_ID_1)

            val expiredChallengeEntity =
                ChallengeEntity(
                    challengeId = CHALLENGE_ID_1,
                    challengerEntitiesMap =
                        mapOf(
                            USER_ID_1 to
                                ChallengerEntity(
                                    userId = USER_ID_1,
                                    displayName = DISPLAY_NAME_1,
                                    isInstigator = true,
                                    status = ChallengerStatus.Pending,
                                ),
                            USER_ID_2 to
                                ChallengerEntity(
                                    userId = USER_ID_2,
                                    displayName = DISPLAY_NAME_2,
                                    isInstigator = false,
                                    status = ChallengerStatus.Pending,
                                ),
                        ),
                    startTimeMillis = 1000L,
                    endTimeMillis = 1L,
                )

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns expiredChallengeEntity

            every { root.updateChildren(any()) } returns Tasks.forResult(null)

            val mailboxUser2Reference = mockk<DatabaseReference>()
            val mailboxSnapshotUser1 = mockk<DataSnapshot>()
            val mailboxSnapshotUser2 = mockk<DataSnapshot>()
            val travelerUser2Reference = mockk<DatabaseReference>()
            val travelerUser2TransactionHandlerSlot = slot<Transaction.Handler>()
            val mutableDataUser2 = mockk<MutableData>()

            every { mailboxRef.child(USER_ID_2) } returns mailboxUser2Reference
            every { mailboxUser2Reference.get() } returns Tasks.forResult(mailboxSnapshotUser2)
            every { userChallengeMailboxReference.get() } returns
                Tasks.forResult(mailboxSnapshotUser1)
            every { mailboxSnapshotUser1.getValue(any<GenericTypeIndicator<String>>()) } returns
                CHALLENGE_ID_1
            every { mailboxSnapshotUser2.getValue(any<GenericTypeIndicator<String>>()) } returns
                CHALLENGE_ID_1

            every { travelersRef.child(USER_ID_2) } returns travelerUser2Reference
            every { mutableDataUser2.getValue(any<GenericTypeIndicator<TravelerEntity>>()) } returns
                null
            every { mutableDataUser2.value = any() } just Runs
            every {
                travelerUser2Reference.runTransaction(capture(travelerUser2TransactionHandlerSlot))
            } answers
                {
                    val result =
                        travelerUser2TransactionHandlerSlot.captured.doTransaction(mutableDataUser2)
                    travelerUser2TransactionHandlerSlot.captured.onComplete(
                        null,
                        result.isSuccess,
                        mockk(),
                    )
                }

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 1) { root.updateChildren(any()) }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
        }

    @Test
    fun `connectTravelerPresence sets traveler busy when challenge exists and is not expired`() =
        runTest {
            setUpConnectTravelerPresence(mailboxChallengeId = CHALLENGE_ID_1)

            val activeChallengeEntity =
                ChallengeEntity(
                    challengeId = CHALLENGE_ID_1,
                    challengerEntitiesMap =
                        mapOf(
                            USER_ID_1 to
                                ChallengerEntity(
                                    userId = USER_ID_1,
                                    displayName = DISPLAY_NAME_1,
                                    isInstigator = true,
                                    status = ChallengerStatus.Pending,
                                ),
                            USER_ID_2 to
                                ChallengerEntity(
                                    userId = USER_ID_2,
                                    displayName = DISPLAY_NAME_2,
                                    isInstigator = false,
                                    status = ChallengerStatus.Pending,
                                ),
                        ),
                    startTimeMillis = 1000L,
                    endTimeMillis = Long.MAX_VALUE,
                )

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns activeChallengeEntity

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { challengesRef.child(CHALLENGE_ID_1) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 0) { userChallengeMailboxReference.removeValue() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    private fun setUpConnectTravelerPresence(mailboxChallengeId: String?) {
        every { travelersRef.child(USER_ID_1) } returns travelerReference
        every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference

        every { connectedRef.addValueEventListener(capture(connectedListenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }
        every { connectedSnapshot.getValue(any<GenericTypeIndicator<Boolean>>()) } returns true

        every { travelerReference.onDisconnect() } returns onDisconnect
        every { onDisconnect.removeValue() } returns Tasks.forResult(null)

        every { userChallengeMailboxReference.get() } returns Tasks.forResult(mailboxSnapshot)
        every { mailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
            mailboxChallengeId

        every { mutableData.getValue(any<GenericTypeIndicator<TravelerEntity>>()) } returns null
        every { mutableData.value = any() } just Runs

        every { travelerReference.runTransaction(capture(transactionHandlerSlot)) } answers
            {
                val result = transactionHandlerSlot.captured.doTransaction(mutableData)
                transactionHandlerSlot.captured.onComplete(null, result.isSuccess, mockk())
            }
    }

    companion object {
        private const val USER_ID_1 = "user-1"
        private const val USER_ID_2 = "user-2"
        private const val DISPLAY_NAME_1 = "Alice"
        private const val DISPLAY_NAME_2 = "Bob"
        private const val CHALLENGE_ID_1 = "challenge-1"
    }
}
