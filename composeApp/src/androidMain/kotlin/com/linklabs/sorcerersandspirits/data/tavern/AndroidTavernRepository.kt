package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.OnDisconnect
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.data.getChildrenFlow
import com.linklabs.sorcerersandspirits.data.getValueFlow
import com.linklabs.sorcerersandspirits.data.model.ChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.data.model.toDomain
import com.linklabs.sorcerersandspirits.data.model.toEntity
import com.linklabs.sorcerersandspirits.domain.tavern.Cancelable
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class AndroidTavernRepository : TavernRepository {

    private val database = FirebaseDatabase.getInstance()

    private val root = database.reference
    private val challengesRef = root.child(CHALLENGES_PATH)
    private val mailboxesRef = root.child(CHALLENGE_MAILBOXES_PATH)
    private val travelersRef = root.child(TAVERN_TRAVELERS_PATH)

    private val disconnectCleanupMutex = Mutex()
    private val disconnectCleanupByUserId = mutableMapOf<String, RegisteredDisconnectCleanup>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveChallengeFlow(userId: String): Flow<Challenge?> =
        getMailboxFlow(userId = userId).flatMapLatest { challengeId ->
            if (challengeId.isNullOrBlank()) {
                flow {
                    clearRegisteredDisconnectCleanup(userId = userId)
                    emit(value = null)
                }
            } else {
                getChallengeFlow(userId = userId, challengeId = challengeId)
            }
        }

    override fun getTavernTravelersFlow(): Flow<List<Traveler>> =
        travelersRef.getChildrenFlow<TravelerEntity>().map { travelerEntities ->
            travelerEntities.map { travelerEntity -> travelerEntity.toDomain() }
        }

    override fun connectTravelerPresence(userId: String, displayName: String): Cancelable {
        val connectedRef = database.getReference(".info/connected")
        val travelerRef = travelersRef.child(userId)

        val listener =
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue<Boolean>() ?: false
                    if (!connected) return

                    travelerRef.onDisconnect().removeValue()

                    travelerRef.runTransaction(
                        object : Transaction.Handler {
                            override fun doTransaction(
                                currentData: MutableData
                            ): Transaction.Result {
                                val current = currentData.getValue(TravelerEntity::class.java)

                                currentData.value =
                                    TravelerEntity(
                                        userId = userId,
                                        displayName = displayName,
                                        tavernStatus =
                                            current?.tavernStatus ?: TavernStatus.Available,
                                    )

                                return Transaction.success(currentData)
                            }

                            override fun onComplete(
                                error: DatabaseError?,
                                committed: Boolean,
                                currentData: DataSnapshot?,
                            ) = Unit
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) = Unit
            }

        connectedRef.addValueEventListener(listener)

        return Cancelable {
            connectedRef.removeEventListener(listener)
            travelerRef.onDisconnect().cancel()
        }
    }

    override suspend fun sendChallenge(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult {
        // TODO: use push() for key generation
        val challengeId = UUID.randomUUID().toString()
        val participants =
            (listOf(element = instigator) + invitees).distinctBy { challenger -> challenger.userId }
        val participantUserIds = participants.map { challenger -> challenger.userId }
        val claimedUserIds = mutableListOf<String>()

        try {
            participants.forEach { participant ->
                val claimed = tryClaimTraveler(userId = participant.userId)

                if (!claimed) {
                    releaseClaims(userIds = claimedUserIds)

                    return SendChallengeResult.TravelerUnavailable(participant.userId)
                }

                claimedUserIds += participant.userId
            }

            registerDisconnectCleanup(
                userId = instigator.userId,
                challengeId = challengeId,
                participantUserIds = participantUserIds,
            )

            val challenge =
                ChallengeEntity(
                    challengeId = challengeId,
                    challengers =
                        participants.associate { challenger ->
                            challenger.userId to challenger.toEntity()
                        },
                )

            val updates =
                buildMap<String, Any?> {
                    put(key = "$CHALLENGES_PATH/$challengeId", value = challenge)

                    participants.forEach { participant ->
                        put(
                            key = "$CHALLENGE_MAILBOXES_PATH/${participant.userId}",
                            value = challengeId,
                        )
                    }
                }

            root.updateChildren(updates).await()

            return SendChallengeResult.Success(challengeId)
        } catch (throwable: Throwable) {
            clearRegisteredDisconnectCleanup(userId = instigator.userId)
            clearChallengeData(challengeId = challengeId, participantUserIds = participantUserIds)
            releaseClaims(userIds = claimedUserIds)
            throw throwable
        }
    }

    override suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        challengesRef
            .child(challengeId)
            .child("challengers")
            .child(userId)
            .child("status")
            .setValue(status.name)
            .await()

        if (status == ChallengerStatus.Declined) {
            mailboxesRef.child(userId).removeValue().await()
            setTravelerAvailableIfPresent(userId = userId)
        }

        reconcileChallenge(challengeId = challengeId)
    }

    override suspend fun finishChallenge(challenge: Challenge) {
        val participantUserIds = challenge.challengers.keys.toList()

        clearRegisteredDisconnectCleanups(userIds = participantUserIds)
        clearChallengeData(
            challengeId = challenge.challengeId,
            participantUserIds = participantUserIds,
        )
        releaseClaims(userIds = participantUserIds)
    }

    private fun getMailboxFlow(userId: String): Flow<String?> =
        mailboxesRef.child(userId).getValueFlow<String>()

    private fun getChallengeFlow(userId: String, challengeId: String): Flow<Challenge?> =
        callbackFlow {
            val challengeRef = challengesRef.child(challengeId)

            val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val challengeEntity = snapshot.getValue<ChallengeEntity>()
                        val challenge = challengeEntity?.toDomain()

                        launch(start = CoroutineStart.UNDISPATCHED) {
                            if (challengeEntity == null) {
                                clearRegisteredDisconnectCleanup(userId = userId)
                            } else {
                                registerDisconnectCleanup(
                                    userId = userId,
                                    challengeId = challengeId,
                                    participantUserIds = challengeEntity.challengers.keys.toList(),
                                )
                            }
                        }

                        trySend(element = challenge)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(cause = error.toException())
                    }
                }

            challengeRef.addValueEventListener(listener)

            awaitClose { challengeRef.removeEventListener(listener) }
        }

    private suspend fun reconcileChallenge(challengeId: String) {
        val challengeEntity =
            challengesRef.child(challengeId).get().await().getValue<ChallengeEntity>() ?: return

        val challengers = challengeEntity.challengers
        val participantUserIds = challengers.keys.toList()

        val nonDeclinedStatuses =
            challengers.values
                .map { challengerEntity -> challengerEntity.status }
                .filterNot { status -> status == ChallengerStatus.Declined }

        val hasEveryoneAccepted =
            nonDeclinedStatuses.isNotEmpty() &&
                nonDeclinedStatuses.all { status -> status == ChallengerStatus.Accepted }

        val doesNotHaveEnoughPlayersLeft = nonDeclinedStatuses.size < MINIMUM_CHALLENGERS

        when {
            hasEveryoneAccepted -> {
                clearRegisteredDisconnectCleanups(userIds = participantUserIds)
                clearChallengeData(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
            }

            doesNotHaveEnoughPlayersLeft -> {
                clearRegisteredDisconnectCleanups(userIds = participantUserIds)
                clearChallengeData(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
                releaseClaims(userIds = participantUserIds)
            }
        }
    }

    private suspend fun tryClaimTraveler(userId: String): Boolean {
        return travelersRef.child(userId).runTransactionAwait { currentData ->
            val traveler =
                currentData.getValue<TravelerEntity>()
                    ?: return@runTransactionAwait Transaction.abort()

            if (traveler.tavernStatus != TavernStatus.Available) {
                return@runTransactionAwait Transaction.abort()
            }

            currentData.value = traveler.copy(tavernStatus = TavernStatus.Busy)

            Transaction.success(currentData)
        }
    }

    override suspend fun setTravelerAvailableIfPresent(userId: String) {
        travelersRef.child(userId).runTransactionAwait { currentData ->
            val traveler =
                currentData.getValue(TravelerEntity::class.java)
                    ?: return@runTransactionAwait Transaction.success(currentData)

            currentData.value = traveler.copy(tavernStatus = TavernStatus.Available)

            Transaction.success(currentData)
        }
    }

    private suspend fun releaseClaims(userIds: List<String>) {
        userIds.forEach { userId -> setTravelerAvailableIfPresent(userId = userId) }
    }

    private suspend fun clearChallengeData(challengeId: String, participantUserIds: List<String>) {
        val updates =
            buildMap<String, Any?> {
                put(key = "$CHALLENGES_PATH/$challengeId", value = null)

                participantUserIds.forEach { userId ->
                    put(key = "$CHALLENGE_MAILBOXES_PATH/$userId", value = null)
                }
            }

        root.updateChildren(updates).await()
    }

    private suspend fun registerDisconnectCleanup(
        userId: String,
        challengeId: String,
        participantUserIds: List<String>,
    ) {
        val existingCleanup = disconnectCleanupMutex.withLock { disconnectCleanupByUserId[userId] }

        if (existingCleanup?.challengeId == challengeId) return

        clearRegisteredDisconnectCleanup(userId = userId)

        val operations = buildList {
            add(
                challengesRef.child(challengeId).onDisconnect().also { onDisconnect ->
                    onDisconnect.removeValue().await()
                }
            )

            participantUserIds.forEach { participantUserId ->
                add(
                    mailboxesRef.child(participantUserId).onDisconnect().also { onDisconnect ->
                        onDisconnect.removeValue().await()
                    }
                )
            }
        }

        disconnectCleanupMutex.withLock {
            disconnectCleanupByUserId[userId] =
                RegisteredDisconnectCleanup(challengeId = challengeId, operations = operations)
        }
    }

    private suspend fun clearRegisteredDisconnectCleanup(userId: String) {
        val cleanup =
            disconnectCleanupMutex.withLock { disconnectCleanupByUserId.remove(userId) } ?: return

        cleanup.operations.forEach { onDisconnect -> onDisconnect.cancel().await() }
    }

    private suspend fun clearRegisteredDisconnectCleanups(userIds: List<String>) {
        userIds.forEach { userId -> clearRegisteredDisconnectCleanup(userId = userId) }
    }

    private suspend fun DatabaseReference.runTransactionAwait(
        block: (MutableData) -> Transaction.Result
    ): Boolean = suspendCancellableCoroutine { continuation ->
        runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result =
                    block(currentData)

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        else -> continuation.resume(value = committed)
                    }
                }
            }
        )
    }

    private data class RegisteredDisconnectCleanup(
        val challengeId: String,
        val operations: List<OnDisconnect>,
    )

    private companion object {
        private const val MINIMUM_CHALLENGERS = 2
    }
}
