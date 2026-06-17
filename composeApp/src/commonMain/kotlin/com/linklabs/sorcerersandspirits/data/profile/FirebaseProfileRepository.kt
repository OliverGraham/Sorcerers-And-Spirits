package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseProfileRepository(database: FirebaseFirestore) : ProfileRepository {

    // TODO(SS-039): implement user names collection to help with keeping a unique name for everyone

    private val profilesCollection = database.collection(collectionPath = FireStoreKeys.PROFILES)

    override suspend fun createProfile(profile: Profile) {
        val profileEntity = profile.toEntity()
        profilesCollection.document(documentPath = profileEntity.userId).set(data = profileEntity)
    }

    override suspend fun deleteProfile(userId: String) {
        profilesCollection.document(documentPath = userId).delete()
    }

    override suspend fun getProfile(userId: String): Profile? {
        val profileDocument = profilesCollection.document(documentPath = userId).get()

        return profileDocument.data<ProfileEntity?>()?.toDomain()
    }

    override fun getProfileFlow(userId: String): Flow<Profile?> =
        profilesCollection.document(documentPath = userId).snapshots.map { snapshot ->
            snapshot.data<ProfileEntity?>()?.toDomain()
        }

    override suspend fun updateDisplayName(userId: String, newDisplayName: String) {
        profilesCollection.document(documentPath = userId).updateFields {
            FireStoreKeys.DISPLAY_NAME to newDisplayName
        }
    }

    override suspend fun incrementStats(userId: String, delta: ProfileStats) {
        val increments = delta.toStatIncrements()
        if (increments.isEmpty()) return

        profilesCollection.document(documentPath = userId).updateFields {
            increments.forEach { (statKey, amount) ->
                FieldPath(FireStoreKeys.STATS, statKey) to FieldValue.increment(value = amount)
            }
        }
    }
}
