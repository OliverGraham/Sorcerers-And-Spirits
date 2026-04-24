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
import com.linklabs.sorcerersandspirits.data.model.toEntity
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

// TODO: Maybe update the AI slop

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
                displayName = DISPLAY_NAME_1,
                tavernStatus = TavernStatus.Available,
            )
        val travelerEntity2 =
            TravelerEntity(
                userId = USER_ID_2,
                displayName = DISPLAY_NAME_2,
                tavernStatus = TavernStatus.Busy,
            )

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
                        displayName = DISPLAY_NAME_1,
                        tavernStatus = TavernStatus.Available,
                    ),
                    Traveler(
                        userId = USER_ID_2,
                        displayName = DISPLAY_NAME_2,
                        tavernStatus = TavernStatus.Busy,
                    ),
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
            val userChallengeMailboxReference = mockk<DatabaseReference>()
            val challengeIdRef = mockk<DatabaseReference>()

            val mailboxListenerSlot = slot<ValueEventListener>()
            val challengeListenerSlot = slot<ValueEventListener>()

            val mailboxSnapshot = mockk<DataSnapshot>()
            val challengeSnapshot = mockk<DataSnapshot>()

            val challengeEntity =
                ChallengeEntity(
                    challengeId = CHALLENGE_ID_1,
                    challengerEntitiesMap =
                        mapOf(
                            USER_ID_1 to
                                ChallengerEntity(
                                    userId = USER_ID_1,
                                    displayName = DISPLAY_NAME_1,
                                    isInstigator = true,
                                    status = ChallengerStatus.Accepted,
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
                    endTimeMillis = 2000L,
                )

            every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeIdRef

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
                CHALLENGE_ID_1
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
    fun `getActiveChallengeFlow switches to latest mailbox challenge id`() = runTest {
        val userChallengeMailboxReference = mockk<DatabaseReference>()
        val challenge1Ref = mockk<DatabaseReference>()
        val challenge2Ref = mockk<DatabaseReference>()

        val mailboxListenerSlot = slot<ValueEventListener>()
        val challenge1ListenerSlot = slot<ValueEventListener>()
        val challenge2ListenerSlot = slot<ValueEventListener>()

        val mailboxSnapshot1 = mockk<DataSnapshot>()
        val mailboxSnapshot2 = mockk<DataSnapshot>()
        val challengeSnapshot1 = mockk<DataSnapshot>()
        val challengeSnapshot2 = mockk<DataSnapshot>()

        val challengeEntity1 =
            ChallengeEntity(
                challengeId = "challenge-1",
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
                endTimeMillis = 2000L,
            )

        val challengeEntity2 =
            ChallengeEntity(
                challengeId = "challenge-2",
                challengerEntitiesMap =
                    mapOf(
                        USER_ID_1 to
                            ChallengerEntity(
                                userId = USER_ID_1,
                                displayName = DISPLAY_NAME_1,
                                isInstigator = true,
                                status = ChallengerStatus.Accepted,
                            ),
                        USER_ID_3 to
                            ChallengerEntity(
                                userId = USER_ID_3,
                                displayName = DISPLAY_NAME_3,
                                isInstigator = false,
                                status = ChallengerStatus.Pending,
                            ),
                    ),
                startTimeMillis = 3000L,
                endTimeMillis = 4000L,
            )

        every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
        every {
            userChallengeMailboxReference.addValueEventListener(capture(mailboxListenerSlot))
        } answers { firstArg<ValueEventListener>() }
        every {
            userChallengeMailboxReference.removeEventListener(any<ValueEventListener>())
        } returns Unit

        every { challengesRef.child("challenge-1") } returns challenge1Ref
        every { challengesRef.child("challenge-2") } returns challenge2Ref

        every { challenge1Ref.addValueEventListener(capture(challenge1ListenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }
        every { challenge2Ref.addValueEventListener(capture(challenge2ListenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }
        every { challenge1Ref.removeEventListener(any<ValueEventListener>()) } returns Unit
        every { challenge2Ref.removeEventListener(any<ValueEventListener>()) } returns Unit

        every { mailboxSnapshot1.getValue(any<GenericTypeIndicator<String>>()) } returns
            "challenge-1"
        every { mailboxSnapshot2.getValue(any<GenericTypeIndicator<String>>()) } returns
            "challenge-2"

        every { challengeSnapshot1.getValue(any<GenericTypeIndicator<ChallengeEntity>>()) } returns
            challengeEntity1
        every { challengeSnapshot2.getValue(any<GenericTypeIndicator<ChallengeEntity>>()) } returns
            challengeEntity2

        repository.getActiveChallengeFlow(userId = USER_ID_1).test {
            mailboxListenerSlot.captured.onDataChange(mailboxSnapshot1)
            challenge1ListenerSlot.captured.onDataChange(challengeSnapshot1)
            assertEquals(challengeEntity1.toDomain(), awaitItem())

            mailboxListenerSlot.captured.onDataChange(mailboxSnapshot2)
            challenge2ListenerSlot.captured.onDataChange(challengeSnapshot2)
            assertEquals(challengeEntity2.toDomain(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { challenge1Ref.removeEventListener(any<ValueEventListener>()) }
    }

    @Test
    fun `getActiveChallengeFlow emits null when challenge entity becomes null`() = runTest {
        val userChallengeMailboxReference = mockk<DatabaseReference>()
        val challengeIdRef = mockk<DatabaseReference>()

        val mailboxListenerSlot = slot<ValueEventListener>()
        val challengeListenerSlot = slot<ValueEventListener>()

        val mailboxSnapshot = mockk<DataSnapshot>()
        val challengeSnapshot = mockk<DataSnapshot>()

        every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
        every { challengesRef.child(CHALLENGE_ID_1) } returns challengeIdRef

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
            CHALLENGE_ID_1
        every { challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>()) } returns
            null

        repository.getActiveChallengeFlow(userId = USER_ID_1).test {
            mailboxListenerSlot.captured.onDataChange(mailboxSnapshot)
            challengeListenerSlot.captured.onDataChange(challengeSnapshot)

            assertEquals(null, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getActiveChallengeFlow emits null when mailbox changes from challenge id to null`() =
        runTest {
            val userChallengeMailboxReference = mockk<DatabaseReference>()
            val challenge1Ref = mockk<DatabaseReference>()

            val mailboxListenerSlot = slot<ValueEventListener>()
            val challenge1ListenerSlot = slot<ValueEventListener>()

            val mailboxSnapshot1 = mockk<DataSnapshot>()
            val mailboxSnapshot2 = mockk<DataSnapshot>()
            val challengeSnapshot1 = mockk<DataSnapshot>()

            val challengeEntity1 =
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
                    endTimeMillis = 2000L,
                )

            every { mailboxRef.child(USER_ID_1) } returns userChallengeMailboxReference
            every {
                userChallengeMailboxReference.addValueEventListener(capture(mailboxListenerSlot))
            } answers { firstArg<ValueEventListener>() }
            every {
                userChallengeMailboxReference.removeEventListener(any<ValueEventListener>())
            } returns Unit

            every { challengesRef.child(CHALLENGE_ID_1) } returns challenge1Ref
            every { challenge1Ref.addValueEventListener(capture(challenge1ListenerSlot)) } answers
                {
                    firstArg<ValueEventListener>()
                }
            every { challenge1Ref.removeEventListener(any<ValueEventListener>()) } returns Unit

            every { mailboxSnapshot1.getValue(any<GenericTypeIndicator<String>>()) } returns
                CHALLENGE_ID_1
            every { mailboxSnapshot2.getValue(any<GenericTypeIndicator<String>>()) } returns null
            every {
                challengeSnapshot1.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns challengeEntity1

            repository.getActiveChallengeFlow(userId = USER_ID_1).test {
                mailboxListenerSlot.captured.onDataChange(mailboxSnapshot1)
                challenge1ListenerSlot.captured.onDataChange(challengeSnapshot1)
                assertEquals(challengeEntity1.toDomain(), awaitItem())

                mailboxListenerSlot.captured.onDataChange(mailboxSnapshot2)
                assertEquals(null, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) { challenge1Ref.removeEventListener(any<ValueEventListener>()) }
        }

    @Test
    fun `connectTravelerPresence adds value event listener to connected reference`() = runTest {
        val listenerSlot = slot<ValueEventListener>()

        every { connectedRef.addValueEventListener(capture(listenerSlot)) } answers
            {
                firstArg<ValueEventListener>()
            }

        repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

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
            val getWrittenTravelerEntity =
                setUpConnectTravelerPresenceCommon(mailboxChallengeId = null)

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            assertEquals(
                TravelerEntity(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    tavernStatus = TavernStatus.Available,
                ),
                getWrittenTravelerEntity(),
            )
            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
            verify(exactly = 0) { challengesRef.child(any()) }
        }

    @Test
    fun `connectTravelerPresence clears mailbox and sets traveler available when challenge entity is null`() =
        runTest {
            val getWrittenTravelerEntity =
                setUpConnectTravelerPresenceCommon(mailboxChallengeId = CHALLENGE_ID_1)

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns null
            every { userChallengeMailboxReference.removeValue() } returns Tasks.forResult(null)

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            assertEquals(
                TravelerEntity(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    tavernStatus = TavernStatus.Available,
                ),
                getWrittenTravelerEntity(),
            )
            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { challengesRef.child(CHALLENGE_ID_1) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 1) { userChallengeMailboxReference.removeValue() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
        }

    @Test
    fun `connectTravelerPresence cleans up expired challenge and sets traveler available`() =
        runTest {
            val getWrittenTravelerEntityUser1 =
                setUpConnectTravelerPresenceCommon(mailboxChallengeId = CHALLENGE_ID_1)

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

            val updatesSlot = slot<Map<String, Any?>>()

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns expiredChallengeEntity
            every { root.updateChildren(capture(updatesSlot)) } returns Tasks.forResult(null)

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            assertEquals(USER_ID_1, getWrittenTravelerEntityUser1()?.userId)
            assertEquals(TavernStatus.Available, getWrittenTravelerEntityUser1()?.tavernStatus)

            assertEquals(
                mapOf(
                    CHALLENGE_ID_1.toChallengesPath() to null,
                    USER_ID_1.toChallengeMailBoxesPath() to null,
                    USER_ID_2.toChallengeMailBoxesPath() to null,
                ),
                updatesSlot.captured,
            )

            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 1) { root.updateChildren(any()) }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
        }

    @Test
    fun `connectTravelerPresence sets traveler busy when challenge exists and is not expired`() =
        runTest {
            val getWrittenTravelerEntity =
                setUpConnectTravelerPresenceCommon(mailboxChallengeId = CHALLENGE_ID_1)

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

            assertEquals(
                TravelerEntity(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    tavernStatus = TavernStatus.Busy,
                ),
                getWrittenTravelerEntity(),
            )
            verify(exactly = 1) { travelerReference.onDisconnect() }
            verify(exactly = 1) { onDisconnect.removeValue() }
            verify(exactly = 1) { userChallengeMailboxReference.get() }
            verify(exactly = 1) { challengesRef.child(CHALLENGE_ID_1) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 0) { userChallengeMailboxReference.removeValue() }
            verify(exactly = 1) { travelerReference.runTransaction(any()) }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    @Test
    fun `sendChallenge returns Failure when pushed challenge key is null`() = runTest {
        val instigator =
            Challenger(
                userId = USER_ID_1,
                displayName = DISPLAY_NAME_1,
                isInstigator = true,
                status = ChallengerStatus.Pending,
            )
        val invitee =
            Challenger(
                userId = USER_ID_2,
                displayName = DISPLAY_NAME_2,
                isInstigator = false,
                status = ChallengerStatus.Pending,
            )

        val pushedChallengeRef = mockk<DatabaseReference>()

        every { challengesRef.push() } returns pushedChallengeRef
        every { pushedChallengeRef.key } returns null

        val result = repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

        assertEquals(
            SendChallengeResult.Failure(
                errorMessage = AndroidTavernRepository.SEND_RESULT_FAILURE_MESSAGE
            ),
            result,
        )

        verify(exactly = 1) { challengesRef.push() }
        verify(exactly = 0) { travelersRef.child(any()) }
        verify(exactly = 0) { root.updateChildren(any()) }
    }

    @Test
    fun `sendChallenge returns NotEnoughChallengersAvailable when only one challenger is claimed`() =
        runTest {
            val instigator =
                Challenger(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                )
            val invitee =
                Challenger(
                    userId = USER_ID_2,
                    displayName = DISPLAY_NAME_2,
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                )

            val pushedChallengeRef = mockk<DatabaseReference>()
            val user1TravelerRef = mockk<DatabaseReference>()
            val user2TravelerRef = mockk<DatabaseReference>()
            val user1MailboxRef = mockk<DatabaseReference>()
            val user1MailboxSnapshot = mockk<DataSnapshot>()

            every { challengesRef.push() } returns pushedChallengeRef
            every { pushedChallengeRef.key } returns CHALLENGE_ID_1

            every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
            every { travelersRef.child(USER_ID_2) } returns user2TravelerRef

            stubRunTransactionCommitted(travelerReference = user1TravelerRef)
            stubRunTransactionAborted(travelerReference = user2TravelerRef)

            every { mailboxRef.child(USER_ID_1) } returns user1MailboxRef
            every { user1MailboxRef.get() } returns Tasks.forResult(user1MailboxSnapshot)
            every { user1MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null

            val result =
                repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

            assertEquals(
                expected =
                    SendChallengeResult.NotEnoughChallengersAvailable(
                        unavailableChallengers = listOf(invitee)
                    ),
                actual = result,
            )

            verify(exactly = 1) { challengesRef.push() }
            verify(exactly = 2) { user1TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { user2TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { user1MailboxRef.get() }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    @Test
    fun `sendChallenge returns Success when exactly two challengers are claimed`() = runTest {
        val instigator =
            Challenger(
                userId = USER_ID_1,
                displayName = DISPLAY_NAME_1,
                isInstigator = true,
                status = ChallengerStatus.Accepted,
            )
        val invitee =
            Challenger(
                userId = USER_ID_2,
                displayName = DISPLAY_NAME_2,
                isInstigator = false,
                status = ChallengerStatus.Pending,
            )

        val pushedChallengeRef = mockk<DatabaseReference>()
        val user1TravelerRef = mockk<DatabaseReference>()
        val user2TravelerRef = mockk<DatabaseReference>()
        val updatesSlot = slot<Map<String, Any?>>()

        every { challengesRef.push() } returns pushedChallengeRef
        every { pushedChallengeRef.key } returns CHALLENGE_ID_1

        every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
        every { travelersRef.child(USER_ID_2) } returns user2TravelerRef

        stubRunTransactionCommitted(travelerReference = user1TravelerRef)
        stubRunTransactionCommitted(travelerReference = user2TravelerRef)

        every { root.updateChildren(capture(updatesSlot)) } returns Tasks.forResult(null)

        val result = repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

        assertEquals(SendChallengeResult.Success(challengeId = CHALLENGE_ID_1), result)

        verify(exactly = 1) { challengesRef.push() }
        verify(exactly = 1) { user1TravelerRef.runTransaction(any()) }
        verify(exactly = 1) { user2TravelerRef.runTransaction(any()) }
        verify(exactly = 1) { root.updateChildren(any()) }
        verify(exactly = 0) { mailboxRef.child(any()) }

        val updates = updatesSlot.captured
        assertEquals(CHALLENGE_ID_1, updates[USER_ID_1.toChallengeMailBoxesPath()])
        assertEquals(CHALLENGE_ID_1, updates[USER_ID_2.toChallengeMailBoxesPath()])

        val challengeEntity = updates[CHALLENGE_ID_1.toChallengesPath()] as ChallengeEntity

        assertEquals(CHALLENGE_ID_1, challengeEntity.challengeId)
        assertEquals(
            mapOf(USER_ID_1 to instigator.toEntity(), USER_ID_2 to invitee.toEntity()),
            challengeEntity.challengerEntitiesMap,
        )

        assertNotNull(actual = challengeEntity.endTimeMillis)
        assertNotNull(actual = challengeEntity.startTimeMillis)
        assertTrue(actual = challengeEntity.endTimeMillis > challengeEntity.startTimeMillis)
    }

    @Test
    fun `updateChallengerStatus when invitee changes from Pending to Accepted and all challengers are then accepted removes challenge references`() =
        runTest {
            val updatesSlot = slot<Map<String, Any?>>()

            val setup =
                setUpUpdateChallengerStatusAcceptanceTransitionTest(
                    userId = USER_ID_2,
                    otherChallengers =
                        listOf(
                            Challenger(
                                userId = USER_ID_1,
                                displayName = DISPLAY_NAME_1,
                                isInstigator = true,
                                status = ChallengerStatus.Accepted,
                            )
                        ),
                    updatesSlot = updatesSlot,
                )

            repository.updateChallengerStatus(
                challengeId = CHALLENGE_ID_1,
                userId = USER_ID_2,
                status = ChallengerStatus.Accepted,
            )

            verify(exactly = 1) { setup.statusReference.setValue(ChallengerStatus.Accepted) }
            verify(exactly = 1) { setup.challengeReference.get() }
            verify(exactly = 0) { setup.userMailboxReference.removeValue() }
            verify(exactly = 0) { travelersRef.child(any()) }
            verify(exactly = 1) { root.updateChildren(any()) }

            assertEquals(
                mapOf(
                    CHALLENGE_ID_1.toChallengesPath() to null,
                    USER_ID_1.toChallengeMailBoxesPath() to null,
                    USER_ID_2.toChallengeMailBoxesPath() to null,
                ),
                updatesSlot.captured,
            )
        }

    @Test
    fun `updateChallengerStatus when invitee changes from Pending to Declined and enough challengers remain does not clean up challenge`() =
        runTest {
            // Arrange
            var currentChallengeEntity =
                createChallengeEntity(
                    challengers =
                        listOf(
                            Challenger(
                                userId = USER_ID_1,
                                displayName = DISPLAY_NAME_1,
                                isInstigator = true,
                                status = ChallengerStatus.Accepted,
                            ),
                            Challenger(
                                userId = USER_ID_2,
                                displayName = DISPLAY_NAME_2,
                                isInstigator = false,
                                status = ChallengerStatus.Pending,
                            ),
                            Challenger(
                                userId = USER_ID_3,
                                displayName = DISPLAY_NAME_3,
                                isInstigator = false,
                                status = ChallengerStatus.Pending,
                            ),
                        )
                )

            val setup =
                setUpUpdateChallengerStatusDeclineTransitionTest(
                    userId = USER_ID_2,
                    currentTravelerEntity =
                        TravelerEntity(
                            userId = USER_ID_2,
                            displayName = DISPLAY_NAME_2,
                            tavernStatus = TavernStatus.Busy,
                        ),
                    onStatusUpdated = { newStatus ->
                        currentChallengeEntity =
                            currentChallengeEntity.copy(
                                challengerEntitiesMap =
                                    currentChallengeEntity.challengerEntitiesMap
                                        .toMutableMap()
                                        .apply {
                                            this[USER_ID_2] =
                                                checkNotNull(this[USER_ID_2])
                                                    .copy(status = newStatus)
                                        }
                            )
                    },
                    getChallengeEntity = { currentChallengeEntity },
                )

            // Act
            repository.updateChallengerStatus(
                challengeId = CHALLENGE_ID_1,
                userId = USER_ID_2,
                status = ChallengerStatus.Declined,
            )

            // Assert
            verify(exactly = 1) { setup.statusReference.setValue(ChallengerStatus.Declined) }
            verify(exactly = 1) { setup.userMailboxReference.removeValue() }
            verify(exactly = 1) { setup.userMailboxReference.get() }
            verify(exactly = 1) { setup.userTravelerReference.runTransaction(any()) }
            verify(exactly = 1) { setup.challengeReference.get() }
            verify(exactly = 0) { root.updateChildren(any()) }

            assertEquals(
                ChallengerStatus.Declined,
                currentChallengeEntity.challengerEntitiesMap.getValue(USER_ID_2).status,
            )
            assertEquals(
                TravelerEntity(
                    userId = USER_ID_2,
                    displayName = DISPLAY_NAME_2,
                    tavernStatus = TavernStatus.Available,
                ),
                setup.getWrittenTravelerEntity(),
            )
        }

    @Test
    fun `setTravelerAvailableIfPresent updates traveler to Available when traveler exists`() =
        runTest {
            val userTravelerReference = mockk<DatabaseReference>()

            every { travelersRef.child(USER_ID_1) } returns userTravelerReference
            every { userTravelerReference.runTransaction(any()) } answers
                {
                    val handler = firstArg<Transaction.Handler>()
                    handler.onComplete(null, true, mockk())
                }

            repository.setTravelerAvailableIfPresent(userId = USER_ID_1)

            verify(exactly = 1) { travelersRef.child(USER_ID_1) }
            verify(exactly = 1) { userTravelerReference.runTransaction(any()) }
        }

    @Test
    fun `setTravelerAvailableIfPresent is a no-op when traveler does not exist`() = runTest {
        val userTravelerReference = mockk<DatabaseReference>()

        every { travelersRef.child(USER_ID_1) } returns userTravelerReference
        every { userTravelerReference.runTransaction(any()) } answers
            {
                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, true, mockk())
            }

        repository.setTravelerAvailableIfPresent(userId = USER_ID_1)

        verify(exactly = 1) { travelersRef.child(USER_ID_1) }
        verify(exactly = 1) { userTravelerReference.runTransaction(any()) }
    }

    @Test
    fun `sendChallenge returns Failure and releases claimed travelers when updateChildren fails`() =
        runTest {
            val instigator =
                Challenger(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                )
            val invitee =
                Challenger(
                    userId = USER_ID_2,
                    displayName = DISPLAY_NAME_2,
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                )

            val pushedChallengeRef = mockk<DatabaseReference>()
            val user1TravelerRef = mockk<DatabaseReference>()
            val user2TravelerRef = mockk<DatabaseReference>()
            val user1MailboxRef = mockk<DatabaseReference>()
            val user2MailboxRef = mockk<DatabaseReference>()
            val user1MailboxSnapshot = mockk<DataSnapshot>()
            val user2MailboxSnapshot = mockk<DataSnapshot>()

            every { challengesRef.push() } returns pushedChallengeRef
            every { pushedChallengeRef.key } returns CHALLENGE_ID_1

            every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
            every { travelersRef.child(USER_ID_2) } returns user2TravelerRef

            stubRunTransactionCommitted(travelerReference = user1TravelerRef)
            stubRunTransactionCommitted(travelerReference = user2TravelerRef)

            every { root.updateChildren(any()) } returns
                Tasks.forException(RuntimeException("boom"))

            every { mailboxRef.child(USER_ID_1) } returns user1MailboxRef
            every { mailboxRef.child(USER_ID_2) } returns user2MailboxRef
            every { user1MailboxRef.get() } returns Tasks.forResult(user1MailboxSnapshot)
            every { user2MailboxRef.get() } returns Tasks.forResult(user2MailboxSnapshot)
            every { user1MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null
            every { user2MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null

            val result =
                repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

            assertEquals(
                SendChallengeResult.Failure(
                    errorMessage = AndroidTavernRepository.SEND_RESULT_FAILURE_MESSAGE
                ),
                result,
            )

            verify(exactly = 1) { challengesRef.push() }
            verify(exactly = 2) { user1TravelerRef.runTransaction(any()) }
            verify(exactly = 2) { user2TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { root.updateChildren(any()) }
            verify(exactly = 1) { user1MailboxRef.get() }
            verify(exactly = 1) { user2MailboxRef.get() }
        }

    @Test
    fun `updateChallengerStatus when invitee changes from Pending to Accepted and not everyone is accepted does not clean up challenge`() =
        runTest {
            val challengeReference = mockk<DatabaseReference>()
            val challengersMapReference = mockk<DatabaseReference>()
            val challengerReference = mockk<DatabaseReference>()
            val statusReference = mockk<DatabaseReference>()

            var currentStatus = ChallengerStatus.Pending

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.child(ChallengeEntity.challengerEntitiesMapName) } returns
                challengersMapReference
            every { challengersMapReference.child(USER_ID_2) } returns challengerReference
            every { challengerReference.child(ChallengerEntity.statusName) } returns statusReference

            every { statusReference.setValue(ChallengerStatus.Accepted) } answers
                {
                    currentStatus = ChallengerStatus.Accepted
                    Tasks.forResult(null)
                }

            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } answers
                {
                    createChallengeEntity(
                        challengers =
                            listOf(
                                Challenger(
                                    userId = USER_ID_1,
                                    displayName = DISPLAY_NAME_1,
                                    isInstigator = true,
                                    status = ChallengerStatus.Accepted,
                                ),
                                Challenger(
                                    userId = USER_ID_2,
                                    displayName = DISPLAY_NAME_2,
                                    isInstigator = false,
                                    status = currentStatus,
                                ),
                                Challenger(
                                    userId = USER_ID_3,
                                    displayName = DISPLAY_NAME_3,
                                    isInstigator = false,
                                    status = ChallengerStatus.Pending,
                                ),
                            )
                    )
                }

            repository.updateChallengerStatus(
                challengeId = CHALLENGE_ID_1,
                userId = USER_ID_2,
                status = ChallengerStatus.Accepted,
            )

            verify(exactly = 1) { statusReference.setValue(ChallengerStatus.Accepted) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 0) { mailboxRef.child(any()) }
            verify(exactly = 0) { travelersRef.child(any()) }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    @Test
    fun `updateChallengerStatus when invitee declines and fewer than minimum challengers remain cleans up and releases travelers`() =
        runTest {
            val challengeReference = mockk<DatabaseReference>()
            val challengersMapReference = mockk<DatabaseReference>()
            val challengerReference = mockk<DatabaseReference>()
            val statusReference = mockk<DatabaseReference>()

            val user1MailboxRef = mockk<DatabaseReference>()
            val user2MailboxRef = mockk<DatabaseReference>()
            val user1MailboxSnapshot = mockk<DataSnapshot>()
            val user2MailboxSnapshot = mockk<DataSnapshot>()

            val user1TravelerRef = mockk<DatabaseReference>()
            val user2TravelerRef = mockk<DatabaseReference>()

            val updatesSlot = slot<Map<String, Any?>>()

            var currentChallengeEntity =
                createChallengeEntity(
                    challengers =
                        listOf(
                            Challenger(
                                userId = USER_ID_1,
                                displayName = DISPLAY_NAME_1,
                                isInstigator = true,
                                status = ChallengerStatus.Accepted,
                            ),
                            Challenger(
                                userId = USER_ID_2,
                                displayName = DISPLAY_NAME_2,
                                isInstigator = false,
                                status = ChallengerStatus.Pending,
                            ),
                        )
                )

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.child(ChallengeEntity.challengerEntitiesMapName) } returns
                challengersMapReference
            every { challengersMapReference.child(USER_ID_2) } returns challengerReference
            every { challengerReference.child(ChallengerEntity.statusName) } returns statusReference

            every { statusReference.setValue(ChallengerStatus.Declined) } answers
                {
                    currentChallengeEntity =
                        currentChallengeEntity.copy(
                            challengerEntitiesMap =
                                currentChallengeEntity.challengerEntitiesMap.toMutableMap().apply {
                                    this[USER_ID_2] =
                                        checkNotNull(this[USER_ID_2])
                                            .copy(status = ChallengerStatus.Declined)
                                }
                        )
                    Tasks.forResult(null)
                }

            every { mailboxRef.child(USER_ID_1) } returns user1MailboxRef
            every { mailboxRef.child(USER_ID_2) } returns user2MailboxRef

            every { user1MailboxRef.get() } returns Tasks.forResult(user1MailboxSnapshot)
            every { user2MailboxRef.removeValue() } returns Tasks.forResult(null)
            every { user2MailboxRef.get() } returns Tasks.forResult(user2MailboxSnapshot)

            every { user1MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                CHALLENGE_ID_1
            every { user2MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                null

            every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
            every { travelersRef.child(USER_ID_2) } returns user2TravelerRef
            stubRunTransactionCommitted(travelerReference = user1TravelerRef)
            stubRunTransactionCommitted(travelerReference = user2TravelerRef)

            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } answers { currentChallengeEntity }

            every { root.updateChildren(capture(updatesSlot)) } returns Tasks.forResult(null)

            repository.updateChallengerStatus(
                challengeId = CHALLENGE_ID_1,
                userId = USER_ID_2,
                status = ChallengerStatus.Declined,
            )

            verify(exactly = 1) { statusReference.setValue(ChallengerStatus.Declined) }
            verify(exactly = 1) { user2MailboxRef.removeValue() }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 1) { root.updateChildren(any()) }
            verify(exactly = 1) { user1MailboxRef.get() }
            verify(exactly = 2) { user2MailboxRef.get() }
            verify(exactly = 1) { user1TravelerRef.runTransaction(any()) }
            verify(exactly = 2) { user2TravelerRef.runTransaction(any()) }

            assertEquals(
                mapOf(
                    CHALLENGE_ID_1.toChallengesPath() to null,
                    USER_ID_1.toChallengeMailBoxesPath() to null,
                    USER_ID_2.toChallengeMailBoxesPath() to null,
                ),
                updatesSlot.captured,
            )
        }

    @Test
    fun `connectTravelerPresence creates new TravelerEntity when traveler does not exist`() =
        runTest {
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
            every { mailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns null

            val getWrittenTravelerEntity =
                stubTravelerTransactionWithNullableCurrentEntity(
                    travelerReference = travelerReference,
                    currentTravelerEntity = null,
                )

            repository.connectTravelerPresence(userId = USER_ID_1, displayName = DISPLAY_NAME_1)

            connectedListenerSlot.captured.onDataChange(connectedSnapshot)
            testScope.advanceUntilIdle()

            assertEquals(
                TravelerEntity(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    tavernStatus = TavernStatus.Available,
                ),
                getWrittenTravelerEntity(),
            )
        }

    @Test
    fun `sendChallenge returns NotEnoughChallengersAvailable and does not release claimed traveler when mailbox contains different challenge id`() =
        runTest {
            val instigator =
                Challenger(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                )
            val invitee =
                Challenger(
                    userId = USER_ID_2,
                    displayName = DISPLAY_NAME_2,
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                )

            val pushedChallengeRef = mockk<DatabaseReference>()
            val user1TravelerRef = mockk<DatabaseReference>()
            val user2TravelerRef = mockk<DatabaseReference>()
            val user1MailboxRef = mockk<DatabaseReference>()
            val user1MailboxSnapshot = mockk<DataSnapshot>()

            every { challengesRef.push() } returns pushedChallengeRef
            every { pushedChallengeRef.key } returns CHALLENGE_ID_1

            every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
            every { travelersRef.child(USER_ID_2) } returns user2TravelerRef

            stubRunTransactionCommitted(travelerReference = user1TravelerRef)
            stubRunTransactionAborted(travelerReference = user2TravelerRef)

            every { mailboxRef.child(USER_ID_1) } returns user1MailboxRef
            every { user1MailboxRef.get() } returns Tasks.forResult(user1MailboxSnapshot)
            every { user1MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                "different-challenge-id"

            val result =
                repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

            assertEquals(
                SendChallengeResult.NotEnoughChallengersAvailable(
                    unavailableChallengers = listOf(invitee)
                ),
                result,
            )

            verify(exactly = 1) { user1TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { user2TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { user1MailboxRef.get() }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    @Test
    fun `sendChallenge returns Failure and does not release claimed travelers when updateChildren fails and mailbox contains different challenge ids`() =
        runTest {
            val instigator =
                Challenger(
                    userId = USER_ID_1,
                    displayName = DISPLAY_NAME_1,
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                )
            val invitee =
                Challenger(
                    userId = USER_ID_2,
                    displayName = DISPLAY_NAME_2,
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                )

            val pushedChallengeRef = mockk<DatabaseReference>()
            val user1TravelerRef = mockk<DatabaseReference>()
            val user2TravelerRef = mockk<DatabaseReference>()
            val user1MailboxRef = mockk<DatabaseReference>()
            val user2MailboxRef = mockk<DatabaseReference>()
            val user1MailboxSnapshot = mockk<DataSnapshot>()
            val user2MailboxSnapshot = mockk<DataSnapshot>()

            every { challengesRef.push() } returns pushedChallengeRef
            every { pushedChallengeRef.key } returns CHALLENGE_ID_1

            every { travelersRef.child(USER_ID_1) } returns user1TravelerRef
            every { travelersRef.child(USER_ID_2) } returns user2TravelerRef

            stubRunTransactionCommitted(travelerReference = user1TravelerRef)
            stubRunTransactionCommitted(travelerReference = user2TravelerRef)

            every { root.updateChildren(any()) } returns
                Tasks.forException(RuntimeException("boom"))

            every { mailboxRef.child(USER_ID_1) } returns user1MailboxRef
            every { mailboxRef.child(USER_ID_2) } returns user2MailboxRef
            every { user1MailboxRef.get() } returns Tasks.forResult(user1MailboxSnapshot)
            every { user2MailboxRef.get() } returns Tasks.forResult(user2MailboxSnapshot)
            every { user1MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                "different-challenge-id-1"
            every { user2MailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns
                "different-challenge-id-2"

            val result =
                repository.sendChallenge(instigator = instigator, invitees = listOf(invitee))

            assertEquals(
                SendChallengeResult.Failure(
                    errorMessage = AndroidTavernRepository.SEND_RESULT_FAILURE_MESSAGE
                ),
                result,
            )

            verify(exactly = 1) { user1TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { user2TravelerRef.runTransaction(any()) }
            verify(exactly = 1) { root.updateChildren(any()) }
            verify(exactly = 1) { user1MailboxRef.get() }
            verify(exactly = 1) { user2MailboxRef.get() }
        }

    @Test
    fun `updateChallengerStatus returns after status write when challenge no longer exists`() =
        runTest {
            val challengeReference = mockk<DatabaseReference>()
            val challengersMapReference = mockk<DatabaseReference>()
            val challengerReference = mockk<DatabaseReference>()
            val statusReference = mockk<DatabaseReference>()

            every { challengesRef.child(CHALLENGE_ID_1) } returns challengeReference
            every { challengeReference.child(ChallengeEntity.challengerEntitiesMapName) } returns
                challengersMapReference
            every { challengersMapReference.child(USER_ID_2) } returns challengerReference
            every { challengerReference.child(ChallengerEntity.statusName) } returns statusReference

            every { statusReference.setValue(ChallengerStatus.Accepted) } returns
                Tasks.forResult(null)
            every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
            every {
                challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>())
            } returns null

            repository.updateChallengerStatus(
                challengeId = CHALLENGE_ID_1,
                userId = USER_ID_2,
                status = ChallengerStatus.Accepted,
            )

            verify(exactly = 1) { statusReference.setValue(ChallengerStatus.Accepted) }
            verify(exactly = 1) { challengeReference.get() }
            verify(exactly = 0) { mailboxRef.child(any()) }
            verify(exactly = 0) { travelersRef.child(any()) }
            verify(exactly = 0) { root.updateChildren(any()) }
        }

    @Test
    fun `setTravelerAvailableIfPresent changes traveler from Busy to Available`() = runTest {
        val userTravelerReference = mockk<DatabaseReference>()
        var writtenTravelerEntity: TravelerEntity? = null

        every { travelersRef.child(USER_ID_1) } returns userTravelerReference
        every { userTravelerReference.runTransaction(any()) } answers
            {
                writtenTravelerEntity =
                    TravelerEntity(
                        userId = USER_ID_1,
                        displayName = DISPLAY_NAME_1,
                        tavernStatus = TavernStatus.Available,
                    )

                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, true, mockk())
            }

        repository.setTravelerAvailableIfPresent(userId = USER_ID_1)

        assertEquals(
            TravelerEntity(
                userId = USER_ID_1,
                displayName = DISPLAY_NAME_1,
                tavernStatus = TavernStatus.Available,
            ),
            writtenTravelerEntity,
        )
    }

    private fun stubTravelerTransactionWithNullableCurrentEntity(
        travelerReference: DatabaseReference,
        currentTravelerEntity: TravelerEntity?,
    ): () -> TravelerEntity? {
        var writtenTravelerEntity: TravelerEntity? = null

        every { travelerReference.runTransaction(any()) } answers
            {
                writtenTravelerEntity =
                    currentTravelerEntity?.copy(
                        userId = USER_ID_1,
                        displayName = DISPLAY_NAME_1,
                        tavernStatus = TavernStatus.Available,
                    )
                        ?: TravelerEntity(
                            userId = USER_ID_1,
                            displayName = DISPLAY_NAME_1,
                            tavernStatus = TavernStatus.Available,
                        )

                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, true, mockk())
            }

        return { writtenTravelerEntity }
    }

    private data class UpdateChallengerStatusDeclineTestSetup(
        val challengeReference: DatabaseReference,
        val statusReference: DatabaseReference,
        val userMailboxReference: DatabaseReference,
        val userTravelerReference: DatabaseReference,
        val getWrittenTravelerEntity: () -> TravelerEntity?,
    )

    private fun setUpUpdateChallengerStatusDeclineTransitionTest(
        challengeId: String = CHALLENGE_ID_1,
        userId: String,
        currentTravelerEntity: TravelerEntity,
        onStatusUpdated: (ChallengerStatus) -> Unit,
        getChallengeEntity: () -> ChallengeEntity,
    ): UpdateChallengerStatusDeclineTestSetup {
        val challengeReference = mockk<DatabaseReference>()
        val challengersMapReference = mockk<DatabaseReference>()
        val challengerReference = mockk<DatabaseReference>()
        val statusReference = mockk<DatabaseReference>()
        val userMailboxReference = mockk<DatabaseReference>()
        val userTravelerReference = mockk<DatabaseReference>()
        val mailboxSnapshot = mockk<DataSnapshot>()

        var writtenTravelerEntity: TravelerEntity? = null

        every { challengesRef.child(challengeId) } returns challengeReference
        every { challengeReference.child(ChallengeEntity.challengerEntitiesMapName) } returns
            challengersMapReference
        every { challengersMapReference.child(userId) } returns challengerReference
        every { challengerReference.child(ChallengerEntity.statusName) } returns statusReference

        every { statusReference.setValue(any<ChallengerStatus>()) } answers
            {
                onStatusUpdated(firstArg())
                Tasks.forResult(null)
            }

        every { mailboxRef.child(userId) } returns userMailboxReference
        every { userMailboxReference.removeValue() } returns Tasks.forResult(null)
        every { userMailboxReference.get() } returns Tasks.forResult(mailboxSnapshot)
        every { mailboxSnapshot.getValue(any<GenericTypeIndicator<String>>()) } returns null

        every { travelersRef.child(userId) } returns userTravelerReference
        every { userTravelerReference.runTransaction(any()) } answers
            {
                writtenTravelerEntity =
                    currentTravelerEntity.copy(tavernStatus = TavernStatus.Available)

                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, true, mockk())
            }

        every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
        every { challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>()) } answers
            {
                getChallengeEntity()
            }

        return UpdateChallengerStatusDeclineTestSetup(
            challengeReference = challengeReference,
            statusReference = statusReference,
            userMailboxReference = userMailboxReference,
            userTravelerReference = userTravelerReference,
            getWrittenTravelerEntity = { writtenTravelerEntity },
        )
    }

    private fun setUpUpdateChallengerStatusAcceptanceTransitionTest(
        challengeId: String = CHALLENGE_ID_1,
        userId: String,
        otherChallengers: List<Challenger>,
        updatesSlot: CapturingSlot<Map<String, Any?>>? = null,
    ): UpdateChallengerStatusTestSetup {
        val challengeReference = mockk<DatabaseReference>()
        val challengersMapReference = mockk<DatabaseReference>()
        val challengerReference = mockk<DatabaseReference>()
        val statusReference = mockk<DatabaseReference>()
        val userMailboxReference = mockk<DatabaseReference>()

        var currentStatus = ChallengerStatus.Pending

        every { challengesRef.child(challengeId) } returns challengeReference
        every { challengeReference.child(ChallengeEntity.challengerEntitiesMapName) } returns
            challengersMapReference
        every { challengersMapReference.child(userId) } returns challengerReference
        every { challengerReference.child(ChallengerEntity.statusName) } returns statusReference

        every { statusReference.setValue(ChallengerStatus.Accepted) } answers
            {
                currentStatus = ChallengerStatus.Accepted
                Tasks.forResult(null)
            }

        every { mailboxRef.child(userId) } returns userMailboxReference

        every { challengeReference.get() } returns Tasks.forResult(challengeSnapshot)
        every { challengeSnapshot.getValue(any<GenericTypeIndicator<ChallengeEntity>>()) } answers
            {
                createChallengeEntity(
                    challengeId = challengeId,
                    challengers =
                        otherChallengers +
                            Challenger(
                                userId = userId,
                                displayName = DISPLAY_NAME_2,
                                isInstigator = false,
                                status = currentStatus,
                            ),
                )
            }

        if (updatesSlot != null) {
            every { root.updateChildren(capture(updatesSlot)) } returns Tasks.forResult(null)
        }

        return UpdateChallengerStatusTestSetup(
            challengeReference = challengeReference,
            statusReference = statusReference,
            userMailboxReference = userMailboxReference,
        )
    }

    private fun createChallengeEntity(
        challengeId: String = CHALLENGE_ID_1,
        challengers: List<Challenger>,
        startTimeMillis: Long = 1000L,
        endTimeMillis: Long = Long.MAX_VALUE,
    ): ChallengeEntity =
        ChallengeEntity(
            challengeId = challengeId,
            challengerEntitiesMap =
                challengers.associate { challenger -> challenger.userId to challenger.toEntity() },
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
        )

    private fun stubRunTransactionCommitted(travelerReference: DatabaseReference) {
        every { travelerReference.runTransaction(any()) } answers
            {
                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, true, mockk())
            }
    }

    private fun stubRunTransactionAborted(travelerReference: DatabaseReference) {
        every { travelerReference.runTransaction(any()) } answers
            {
                val handler = firstArg<Transaction.Handler>()
                handler.onComplete(null, false, mockk())
            }
    }

    private fun setUpConnectTravelerPresenceCommon(
        mailboxChallengeId: String?
    ): () -> TravelerEntity? {
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

        return stubTravelerTransaction(
            travelerReference = travelerReference,
            mutableData = mutableData,
            currentTravelerEntity =
                TravelerEntity(
                    userId = USER_ID_1,
                    displayName = "Old Name",
                    tavernStatus = TavernStatus.Busy,
                ),
        )
    }

    private fun stubTravelerTransaction(
        travelerReference: DatabaseReference,
        mutableData: MutableData,
        currentTravelerEntity: TravelerEntity,
    ): () -> TravelerEntity? {
        var writtenTravelerEntity: TravelerEntity? = null
        val transactionHandlerSlot = slot<Transaction.Handler>()

        every { mutableData.getValue(any<GenericTypeIndicator<TravelerEntity>>()) } returns
            currentTravelerEntity
        every { mutableData.value = any() } answers { writtenTravelerEntity = firstArg() }

        every { travelerReference.runTransaction(capture(transactionHandlerSlot)) } answers
            {
                val result = transactionHandlerSlot.captured.doTransaction(mutableData)
                transactionHandlerSlot.captured.onComplete(null, result.isSuccess, mockk())
            }

        return { writtenTravelerEntity }
    }

    private data class UpdateChallengerStatusTestSetup(
        val challengeReference: DatabaseReference,
        val statusReference: DatabaseReference,
        val userMailboxReference: DatabaseReference,
    )

    companion object {
        private const val USER_ID_1 = "user-1"
        private const val DISPLAY_NAME_1 = "Alice"
        private const val USER_ID_2 = "user-2"
        private const val DISPLAY_NAME_2 = "Bob"
        private const val USER_ID_3 = "user-3"
        private const val DISPLAY_NAME_3 = "Charlie"
        private const val CHALLENGE_ID_1 = "challenge-1"
    }
}
