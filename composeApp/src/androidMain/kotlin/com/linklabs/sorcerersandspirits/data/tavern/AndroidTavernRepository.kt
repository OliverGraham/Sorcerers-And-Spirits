package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.data.model.ArenaChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.toTravelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengeStatus
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidTavernRepository : TavernRepository {

    private val database = FirebaseDatabase.getInstance()
    private val tavernReference = database.getReference(FireStoreKeys.TAVERN)
    private val challengesReference = database.getReference(FireStoreKeys.CHALLENGES)

    override fun getTavernFlow(): Flow<Tavern> = callbackFlow {
        val tavernListener = getTavernListener()
        tavernReference.addChildEventListener(tavernListener)
        awaitClose { tavernReference.removeEventListener(tavernListener) }
    }

    override suspend fun aTravelerEntersTavern(traveler: Traveler) {
        val travelerRef = tavernReference.child(traveler.id)
        travelerRef.setValue(traveler)
        travelerRef.onDisconnect().removeValue()
    }

    override fun getArenaChallengeFlow(userId: String?): Flow<ArenaChallenge> = callbackFlow {
        if (userId.isNullOrEmpty()) {
            close()
            return@callbackFlow
        }

        val userInboxReference = challengesReference.child(userId)

        val arenaChallengeListener = getArenaChallengeListener()
        userInboxReference.addChildEventListener(arenaChallengeListener)
        awaitClose { userInboxReference.removeEventListener(arenaChallengeListener) }
    }

    // TODO: Evaluate/add docstring
    override suspend fun sendArenaChallenges(sender: Traveler, receivers: List<Traveler>) {
        // 1. Generate a unique ID for this specific arena session
        val challengeId = challengesReference.push().key ?: return

        // 2. Create the Entity that matches what the listener expects
        val receiverEntities = receivers.map { it.toTravelerEntity() }
        val challengeEntity = ArenaChallengeEntity(
            arenaId = challengeId,
            sender = sender.toTravelerEntity(),
            receivers = receiverEntities,
            status = ChallengeStatus.PENDING,
        )

        receiverEntities.forEach { receiver ->
            challengesReference
                .child(receiver.id) // Target: /challenges/RECEIVER_ID/
                .child(challengeId) // Entry: /challenges/RECEIVER_ID/CHALLENGE_ID
                .setValue(challengeEntity)
        }
    }
}
