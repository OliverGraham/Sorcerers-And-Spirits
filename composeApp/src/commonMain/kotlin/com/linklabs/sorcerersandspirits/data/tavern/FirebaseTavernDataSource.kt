package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import dev.gitlive.firebase.database.ChildEvent
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.database.ServerValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

class FirebaseTavernDataSource(database: FirebaseDatabase) : TavernDataSource {

    private val connectedReference = database.reference(path = CONNECTED_REFERENCE_PATH)

    private val root = database.reference()
    private val tavernTravelersReference = root.child(path = TAVERN_TRAVELERS_PATH)
    private val mailboxChallengesReference = root.child(path = CHALLENGE_MAILBOXES_PATH)
    private val challengesReference = root.child(path = CHALLENGES_PATH)

    override fun travelerEntitiesFlow(): Flow<List<TravelerEntity>> = tavernTravelersReference
        .childEvents(ChildEvent.Type.ADDED, ChildEvent.Type.CHANGED, ChildEvent.Type.REMOVED)
        .scan(initial = emptyMap<String, TravelerEntity>()) { travelersByUserId, childEvent ->
            val userId = childEvent.snapshot.key ?: return@scan travelersByUserId

            if (childEvent.type == ChildEvent.Type.REMOVED) {
                travelersByUserId - userId
            } else {
                val traveler =
                    childEvent.snapshot.value(strategy = TravelerEntity.serializer().nullable)
                if (traveler != null) {
                    travelersByUserId + (userId to traveler)
                } else {
                    travelersByUserId
                }
            }
        }
        .map { travelersByUserId -> travelersByUserId.values.toList() }

    override fun mailboxChallengeIdFlow(userId: String): Flow<String?> = mailboxChallengesReference.child(path = userId).valueEvents.map { snapshot ->
        if (snapshot.exists) snapshot.value(strategy = String.serializer().nullable) else null
    }

    override fun challengeEntityFlow(challengeId: String): Flow<ChallengeEntity?> = challengesReference.child(path = challengeId).valueEvents.map { snapshot ->
        snapshot.value(strategy = ChallengeEntity.serializer().nullable)
    }

    override fun connectedFlow(): Flow<Boolean> = connectedReference.valueEvents.map { snapshot ->
        snapshot.value<Boolean?>()
            ?: false
    }

    override suspend fun getMailboxChallengeId(userId: String): String? = mailboxChallengesReference.child(path = userId).awaitNullableValue<String>()

    override suspend fun getChallengeEntity(challengeId: String): ChallengeEntity? = challengesReference.child(path = challengeId).awaitNullableValue<ChallengeEntity>()

    override fun generateChallengeId(): String? = challengesReference.push().key

    override suspend fun createChallenge(
        challengeId: String,
        challengers: Map<String, ChallengerEntity>,
        participantUserIds: List<String>,
    ) {
        root.updateChildren(
            update =
            buildMap {
                this[challengeId.toChallengesPath()] =
                    ChallengeWriteEntity(
                        challengeId = challengeId,
                        challengerEntitiesMap = challengers,
                    )
                participantUserIds.forEach { userId ->
                    this[userId.toChallengeMailBoxesPath()] = challengeId
                }
            },
        )
    }

    override suspend fun registerTravelerRemovalOnDisconnect(userId: String) {
        tavernTravelersReference.child(path = userId).onDisconnect().removeValue()
    }

    override suspend fun upsertTravelerPresence(
        userId: String,
        displayName: String,
        tavernStatus: TavernStatus,
        rating: Long,
    ) {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable,
        ) { currentTraveler ->
            currentTraveler?.copy(
                userId = userId,
                displayName = displayName,
                tavernStatus = tavernStatus,
                rating = rating,
            )
                ?: TravelerEntity(
                    userId = userId,
                    displayName = displayName,
                    tavernStatus = tavernStatus,
                    rating = rating,
                )
        }
    }

    override suspend fun markTravelerBusyIfAvailable(userId: String): Boolean = try {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable,
        ) { traveler ->
            traveler ?: throw AbortTransactionException()

            if (traveler.tavernStatus != TavernStatus.Available) {
                throw AbortTransactionException()
            }

            traveler.copy(tavernStatus = TavernStatus.Busy)
        }

        true
    } catch (_: AbortTransactionException) {
        false
    }

    override suspend fun setTravelerStatusIfPresent(userId: String, tavernStatus: TavernStatus) {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable,
        ) { traveler ->
            traveler?.copy(tavernStatus = tavernStatus) ?: traveler
        }
    }

    override suspend fun releaseTravelerIfBusy(userId: String) {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable,
        ) { traveler ->
            when {
                traveler == null -> null
                traveler.tavernStatus != TavernStatus.Busy -> traveler
                else -> traveler.copy(tavernStatus = TavernStatus.Available)
            }
        }
    }

    override suspend fun setChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        challengesReference
            .child(path = challengeId)
            .child(path = ChallengeEntity.challengerEntitiesMapName)
            .child(path = userId)
            .child(path = ChallengerEntity.statusName)
            .setValue(value = status)
    }

    override suspend fun removeMailbox(userId: String) {
        mailboxChallengesReference.child(path = userId).removeValue()
    }

    override suspend fun applyUpdates(updates: Map<String, Any?>) {
        root.updateChildren(update = updates)
    }

    private class AbortTransactionException : RuntimeException()

    companion object {
        const val CONNECTED_REFERENCE_PATH = ".info/connected"
    }
}

@Serializable
private data class ChallengeWriteEntity(
    val challengeId: String,
    val challengerEntitiesMap: Map<String, ChallengerEntity>,
    val startTimeMillis: ServerValue = ServerValue.TIMESTAMP,
)

private suspend inline fun <reified T> DatabaseReference.awaitNullableValue(): T? {
    val snapshot = valueEvents.first()

    return if (snapshot.exists) {
        snapshot.value<T>()
    } else {
        null
    }
}
