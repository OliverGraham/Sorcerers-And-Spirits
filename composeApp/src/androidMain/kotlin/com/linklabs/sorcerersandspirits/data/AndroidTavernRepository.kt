package com.linklabs.sorcerersandspirits.data

import android.system.Os.close
import androidx.core.app.PendingIntentCompat.send
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class AndroidTavernRepository : TavernRepository {

    private val database = FirebaseDatabase.getInstance()
    private val tavernReference = database.getReference(TAVERN)

    override fun getTavernFlow(): Flow<Tavern> = callbackFlow {
        val tavernListener = getTavernListener()
        tavernReference.addChildEventListener(tavernListener)
        awaitClose { tavernReference.removeEventListener(tavernListener) }
    }

    private fun ProducerScope<Tavern>.getTavernListener() = object : ChildEventListener {
        val travelersInTavern = mutableListOf<Traveler>()

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val traveler = snapshot.toTraveler() ?: return

            travelersInTavern.add(element = traveler)
            launch {
                send(
                    element = Tavern(travelers = travelersInTavern.toList()),
                )
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {
            val traveler = snapshot.toTraveler() ?: return

            travelersInTavern.remove(element = traveler)
            launch {
                send(
                    element = Tavern(travelers = travelersInTavern.toList()),
                )
            }
        }
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }

    private fun DataSnapshot.toTraveler(): Traveler? {
        val travelerEntity = getValue(TravelerEntity::class.java) ?: return null
        return travelerEntity.toTraveler()
    }

    override suspend fun aTravelerEntersTavern(traveler: Traveler) {
        val travelerRef = tavernReference.child(traveler.id)
        travelerRef.setValue(traveler)
        travelerRef.onDisconnect().removeValue()
    }
}
