package com.linklabs.sorcerersandspirits.data

import com.google.firebase.firestore.FirebaseFirestore
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AndroidTavernRepository : TavernRepository {

    private val database = FirebaseFirestore.getInstance()

    override fun getTavernFlow(): Flow<Tavern> = callbackFlow {
        val listener = database.collection(TAVERN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(cause = error)
                    return@addSnapshotListener
                }

                val tavern = snapshot?.tavernCollectionToTavern() ?: return@addSnapshotListener
                launch {
                    send(element = tavern)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun aTravelerEntersTavern(traveler: Traveler) {
        database
            .collection(TAVERN)
            .document(traveler.id)
            .set(traveler)
            .await()
    }

    override suspend fun leaveTavern(id: String) {
        database
            .collection(TAVERN)
            .document(id)
            .delete()
            .await()
    }
}
