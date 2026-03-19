package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.linklabs.sorcerersandspirits.data.model.ArenaChallengeEntity
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

// TODO: Double check what is necessary, considering new Challenges approach

internal fun ProducerScope<ArenaChallenge>.getArenaChallengeListener() = object : ChildEventListener {

    // We track active challenges in a map keyed by their unique DB push ID
    private val activeChallenges = mutableMapOf<String, ArenaChallenge>()

    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        val challenge = snapshot.getValue(ArenaChallengeEntity::class.java) ?: return
        val challengeId = snapshot.key ?: return

        val arenaChallenge = challenge.toArenaChallenge() ?: return

        // Logic: Only notify the UI if this user is involved
        // Note: You'll need the current user's ID here, usually passed into the function
        activeChallenges[challengeId] = arenaChallenge

        launch {
            send(element = arenaChallenge)
        }
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        val updatedChallenge = snapshot.getValue(ArenaChallengeEntity::class.java) ?: return
        val challengeId = snapshot.key ?: return

        val arenaChallenge = updatedChallenge.toArenaChallenge() ?: return

        activeChallenges[challengeId] = arenaChallenge

        launch {
            // This is key: when status changes to "ACCEPTED",
            // the UI collector will see this and navigate to the Arena screen
            send(element = arenaChallenge)
        }
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        val challengeId = snapshot.key ?: return
        activeChallenges.remove(challengeId)
        // Optionally send a null or an empty state if the challenge is cancelled
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

    override fun onCancelled(error: DatabaseError) {
        close(error.toException())
    }
}
