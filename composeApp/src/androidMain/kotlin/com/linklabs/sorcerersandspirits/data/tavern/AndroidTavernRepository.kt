package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.data.model.ArenaChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.toArenaChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.toTravelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
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

    override fun aTravelerEntersTavern(traveler: Traveler) {
        val travelerRef = tavernReference.child(traveler.id)
        travelerRef.setValue(traveler)
        travelerRef.onDisconnect().removeValue()
    }

    override fun getArenaChallengeFlow(userId: String): Flow<ArenaChallenge> = callbackFlow {
        val userInboxReference = challengesReference.child(userId)
        val arenaChallengeListener = getArenaChallengeListener()

        userInboxReference.addChildEventListener(arenaChallengeListener)
        awaitClose { userInboxReference.removeEventListener(arenaChallengeListener) }
    }

    /**
     * Sends the initial challenge to all receivers and the sender. TODO: Update naming?
     */
    override suspend fun sendArenaChallenges(sender: Traveler, receivers: List<Traveler>) {
        val arenaId = challengesReference.push().key ?: return

        val challengeEntity = ArenaChallengeEntity(
            arenaId = arenaId,
            sender = sender.toTravelerEntity(),
            receivers = receivers.map { it.toTravelerEntity() },
        )
        writeChallengeToMailboxes(challengeEntity = challengeEntity)
    }

    override fun updateArenaChallengeStatus(arenaChallenge: ArenaChallenge) {
        writeChallengeToMailboxes(challengeEntity = arenaChallenge.toArenaChallengeEntity())
    }

    /**
     * Writes challenge to everyone involved (sender and all receivers).
     * Path: /challenges/{userId}/{arenaId}
     */
    private fun writeChallengeToMailboxes(challengeEntity: ArenaChallengeEntity) {
        val participantIds = challengeEntity.receivers.map { it.id } + challengeEntity.sender.id
        val arenaId = challengeEntity.arenaId
        val updates = participantIds.associate { id ->
            "$id/$arenaId" to challengeEntity
        }

        challengesReference.updateChildren(updates)
    }
}
