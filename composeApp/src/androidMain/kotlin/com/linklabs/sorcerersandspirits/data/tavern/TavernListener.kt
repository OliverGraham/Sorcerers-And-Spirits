package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch

internal fun ProducerScope<Tavern>.getTavernListener() =
    object : ChildEventListener {
        val travelersInTavern = mutableListOf<Traveler>()

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val traveler = snapshot.toTraveler() ?: return

            travelersInTavern.add(element = traveler)
            launch { send(element = Tavern(travelers = travelersInTavern.toList())) }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val traveler = snapshot.toTraveler() ?: return

            travelersInTavern.remove(element = traveler)
            launch { send(element = Tavern(travelers = travelersInTavern.toList())) }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
