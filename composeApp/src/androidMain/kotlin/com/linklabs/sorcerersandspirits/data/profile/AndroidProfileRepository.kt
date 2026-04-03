package com.linklabs.sorcerersandspirits.data.profile

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AndroidProfileRepository(database: FirebaseFirestore) : ProfileRepository {
    // TODO: implement user names collection to help with keeping a unique name for everyone

    private val profilesCollection = database.collection(FireStoreKeys.PROFILES)

    override suspend fun createProfile(profile: Profile) {
        val profileEntity = profile.toEntity()
        profilesCollection.document(profileEntity.userId).set(profileEntity).await()
    }

    override suspend fun deleteProfile(userId: String) {
        profilesCollection.document(userId).delete().await()
    }

    override suspend fun getProfile(userId: String): Profile? {
        val profileDocument = profilesCollection.document(userId).get().await()

        val profileEntity = profileDocument.toObject<ProfileEntity>()
        return profileEntity?.toDomain()
    }

    override fun getProfileFlow(userId: String): Flow<Profile?> =
        profilesCollection.document(userId).snapshots().map { snapshot ->
            snapshot.toObject<ProfileEntity>()?.toDomain()
        }

    override suspend fun updateDisplayName(userId: String, newDisplayName: String) {
        profilesCollection
            .document(userId)
            .update(/* field= */ FireStoreKeys.DISPLAY_NAME, /* value= */ newDisplayName)
            .await()
    }
}
