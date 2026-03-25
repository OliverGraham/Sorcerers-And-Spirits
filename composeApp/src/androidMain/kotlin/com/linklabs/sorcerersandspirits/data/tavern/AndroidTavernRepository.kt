package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.data.model.ArenaChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
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
    private val arenaChallengesReference = database.getReference(FireStoreKeys.ARENA_CHALLENGES)

    override fun getTavernFlow(): Flow<Tavern> = callbackFlow {
        val tavernListener = getTavernListener()
        tavernReference.addChildEventListener(tavernListener)
        awaitClose { tavernReference.removeEventListener(tavernListener) }
    }

    override fun aTravelerEntersTavern(traveler: Traveler) {
        val travelerRef = tavernReference.child(traveler.id)
        travelerRef.setValue(traveler.toTravelerEntity())
        travelerRef.onDisconnect().removeValue()
    }

    override fun getArenaChallengeFlow(userId: String): Flow<ArenaChallenge> = callbackFlow {
        val pathOfArenaChallengeThatContainsUser =
            "${ArenaChallengeEntity.getTravelerEntityMapName()}/$userId/${TravelerEntity.getIdPropertyName()}"

        val arenaChallengeEntitiesInDatabase = arenaChallengesReference
            .orderByChild(pathOfArenaChallengeThatContainsUser)
            .equalTo(userId)

        val arenaChallengeListener = getArenaChallengeListener()

        arenaChallengeEntitiesInDatabase.addChildEventListener(arenaChallengeListener)
        awaitClose { arenaChallengeEntitiesInDatabase.removeEventListener(arenaChallengeListener) }
    }

    /**
     * Sends the initial challenge to all receivers and the sender.
     */
    override suspend fun sendArenaChallenges(travelers: List<Traveler>) {
        val arenaId = arenaChallengesReference.push().key ?: return

        ArenaChallenge(
            arenaId = arenaId,
            travelers = travelers,
        ).also { it.syncWithDatabase() }
    }

    override fun updateArenaChallengeStatus(arenaChallenge: ArenaChallenge) {
        arenaChallenge.syncWithDatabase()
    }

    override fun removeArenaChallenge(arenaChallenge: ArenaChallenge) {
        arenaChallenge.syncWithDatabase(shouldDelete = true)
    }

    /**
     * Synchronizes the [ArenaChallenge] state to the Firebase Realtime Database.
     *
     * Because participants use a deep-path query to find this challenge, updating this
     * single node automatically notifies everyone involved.
     *
     * @param shouldDelete If true, removes the challenge from the database.
     */
    private fun ArenaChallenge.syncWithDatabase(shouldDelete: Boolean = false) {
        val entityToWrite = if (shouldDelete) null else toArenaChallengeEntity()

        // encode()??????
        arenaChallengesReference.child(arenaId)
            .setValue(entityToWrite)
    }
}
