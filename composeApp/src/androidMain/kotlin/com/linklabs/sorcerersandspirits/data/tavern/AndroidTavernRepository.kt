package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.data.model.toArenaChallengeEntity
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
     * Sends the initial challenge to all receivers and the sender.
     */
    override suspend fun sendArenaChallenges(sender: Traveler, receivers: List<Traveler>) {
        val arenaId = challengesReference.push().key ?: return

        ArenaChallenge(
            arenaId = arenaId,
            sender = sender,
            receivers = receivers,
        ).also { it.writeChallengeToMailboxes() }
    }

    override fun updateArenaChallengeStatus(arenaChallenge: ArenaChallenge) {
        arenaChallenge.writeChallengeToMailboxes()
    }

    override fun removeArenaChallenge(arenaChallenge: ArenaChallenge) {
        arenaChallenge.writeChallengeToMailboxes(shouldDelete = true)
    }

    /**
     * Writes challenge to everyone involved (sender and all receivers).
     *
     * @param shouldDelete If true, then the challenge is removed from the database by setting to null
     */
    private fun ArenaChallenge.writeChallengeToMailboxes(shouldDelete: Boolean = false) {
        val entityToWrite = if (shouldDelete) null else toArenaChallengeEntity()
        val updates = allParticipantIds.associate { id ->
            "$id/$arenaId" to entityToWrite
        }

        challengesReference.updateChildren(updates)
    }
}
