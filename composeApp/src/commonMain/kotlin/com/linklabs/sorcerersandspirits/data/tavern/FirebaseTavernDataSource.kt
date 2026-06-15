package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

class FirebaseTavernDataSource(database: FirebaseDatabase) : TavernDataSource {

    private val connectedReference = database.reference(path = CONNECTED_REFERENCE_PATH)

    private val root = database.reference()
    private val tavernTravelersReference = root.child(path = TAVERN_TRAVELERS_PATH)
    private val mailboxChallengesReference = root.child(path = CHALLENGE_MAILBOXES_PATH)
    private val challengesReference = root.child(path = CHALLENGES_PATH)

    override fun travelerEntitiesFlow(): Flow<List<TravelerEntity>> =
        tavernTravelersReference.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { child ->
                child.value(strategy = TravelerEntity.serializer().nullable)
            }
        }

    override fun mailboxChallengeIdFlow(userId: String): Flow<String?> =
        mailboxChallengesReference.child(path = userId).valueEvents.map { snapshot ->
            if (snapshot.exists) snapshot.value(strategy = String.serializer().nullable) else null
        }

    override fun challengeEntityFlow(challengeId: String): Flow<ChallengeEntity?> =
        challengesReference.child(path = challengeId).valueEvents.map { snapshot ->
            snapshot.value(strategy = ChallengeEntity.serializer().nullable)
        }

    override fun connectedFlow(): Flow<Boolean> =
        connectedReference.valueEvents.map { snapshot -> snapshot.value<Boolean?>() ?: false }

    override suspend fun getMailboxChallengeId(userId: String): String? =
        mailboxChallengesReference.child(path = userId).awaitNullableValue<String>()

    override suspend fun getChallengeEntity(challengeId: String): ChallengeEntity? =
        challengesReference.child(path = challengeId).awaitNullableValue<ChallengeEntity>()

    override fun generateChallengeId(): String? = challengesReference.push().key

    override suspend fun registerTravelerRemovalOnDisconnect(userId: String) {
        tavernTravelersReference.child(path = userId).onDisconnect().removeValue()
    }

    override suspend fun upsertTravelerPresence(
        userId: String,
        displayName: String,
        tavernStatus: TavernStatus,
    ) {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable
        ) { currentTraveler ->
            currentTraveler?.copy(
                userId = userId,
                displayName = displayName,
                tavernStatus = tavernStatus,
            )
                ?: TravelerEntity(
                    userId = userId,
                    displayName = displayName,
                    tavernStatus = tavernStatus,
                )
        }
    }

    override suspend fun markTravelerBusyIfAvailable(userId: String): Boolean =
        try {
            tavernTravelersReference.child(path = userId).runTransaction(
                strategy = TravelerEntity.serializer().nullable
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
            strategy = TravelerEntity.serializer().nullable
        ) { traveler ->
            traveler?.copy(tavernStatus = tavernStatus) ?: traveler
        }
    }

    override suspend fun releaseTravelerIfBusy(userId: String) {
        tavernTravelersReference.child(path = userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable
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

private suspend inline fun <reified T> DatabaseReference.awaitNullableValue(): T? {
    val snapshot = valueEvents.first()

    return if (snapshot.exists) {
        snapshot.value<T>()
    } else {
        null
    }
}
