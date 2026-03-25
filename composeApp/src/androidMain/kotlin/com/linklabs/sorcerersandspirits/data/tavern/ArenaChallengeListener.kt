package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.model.ArenaChallengeEntity
import com.linklabs.sorcerersandspirits.domain.tavern.ArenaChallenge
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

internal fun ProducerScope<ArenaChallenge>.getArenaChallengeListener() =
    object : ChildEventListener {

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.sendArenaChallenge()
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.sendArenaChallenge()
        }

        private fun DataSnapshot.sendArenaChallenge() {
            val arenaChallengeEntity = getValue<ArenaChallengeEntity>() ?: return
            val arenaChallenge = arenaChallengeEntity.toArenaChallenge() ?: return

            launch { send(element = arenaChallenge) }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
