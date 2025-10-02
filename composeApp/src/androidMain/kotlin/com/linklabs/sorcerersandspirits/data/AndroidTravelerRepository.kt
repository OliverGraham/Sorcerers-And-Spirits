package com.linklabs.sorcerersandspirits.data

import com.google.firebase.firestore.FirebaseFirestore
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TRAVELERS
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerRepository
import kotlinx.coroutines.tasks.await

class AndroidTravelerRepository : TravelerRepository {

    private val database = FirebaseFirestore.getInstance()

    override suspend fun saveTraveler(traveler: Traveler) {
        database
            .collection(TRAVELERS)
            .document(traveler.id)
            .set(traveler)
            .await()
    }

    override suspend fun getTraveler(id: String): Traveler? {
        val travelerDocument = database
            .collection(TRAVELERS)
            .document(id)
            .get()
            .await()

        val travelerEntity = travelerDocument.toObject(TravelerEntity::class.java)
        return travelerEntity?.toTraveler()
    }

    override suspend fun deleteTraveler(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateDisplayName(id: String, displayName: String) {
        database
            .collection(TRAVELERS)
            .document(id)
            .update(
                /* field = */
                "displayName",
                /* value = */
                displayName,
            )
            .await()
    }
}
