package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseProfileRepository(private val database: FirebaseFirestore) : ProfileRepository {

    private val profilesCollection = database.collection(collectionPath = FireStoreKeys.PROFILES)
    private val displayNamesCollection =
        database.collection(collectionPath = FireStoreKeys.DISPLAY_NAMES)

    override suspend fun createProfile(profile: Profile): DisplayNameResult {
        val profileRef = profilesCollection.document(documentPath = profile.userId)
        val nameRef = displayNameReference(displayName = profile.displayName)

        return database.runTransaction {
            if (isReservedByOther(nameRef = nameRef, userId = profile.userId)) {
                return@runTransaction DisplayNameResult.NAME_TAKEN
            }

            set(documentRef = profileRef, data = profile.toEntity())
            reserve(nameRef = nameRef, userId = profile.userId, displayName = profile.displayName)
            DisplayNameResult.SUCCESS
        }
    }

    override suspend fun deleteProfile(userId: String) {
        val profileRef = profilesCollection.document(documentPath = userId)

        database.runTransaction {
            // Firestore requires all reads before any writes, so read the reserved name first.
            val reservedDisplayName =
                get(documentRef = profileRef).data<ProfileEntity?>()?.displayName?.takeIf {
                    it.isNotBlank()
                }

            delete(documentRef = profileRef)

            if (reservedDisplayName != null) {
                delete(documentRef = displayNameReference(displayName = reservedDisplayName))
            }
        }
    }

    override suspend fun getProfile(userId: String): Profile? {
        val profileDocument = profilesCollection.document(documentPath = userId).get()

        return profileDocument.data<ProfileEntity?>()?.toDomain()
    }

    override suspend fun getTopProfiles(limit: Int): List<Profile> =
        profilesCollection
            .orderBy(field = OVERALL_GAUNTLETS_WON_PATH, direction = Direction.DESCENDING)
            .limit(limit = limit)
            .get()
            .documents
            .mapNotNull { document -> document.data<ProfileEntity?>()?.toDomain() }

    override fun getProfileFlow(userId: String): Flow<Profile?> =
        profilesCollection.document(documentPath = userId).snapshots.map { snapshot ->
            snapshot.data<ProfileEntity?>()?.toDomain()
        }

    override suspend fun updateDisplayName(
        userId: String,
        newDisplayName: String,
    ): DisplayNameResult {
        val profileRef = profilesCollection.document(documentPath = userId)
        val newNameRef = displayNameReference(displayName = newDisplayName)

        return database.runTransaction {
            if (isReservedByOther(nameRef = newNameRef, userId = userId)) {
                return@runTransaction DisplayNameResult.NAME_TAKEN
            }

            // Firestore requires all reads before any writes, so read the old name here.
            val currentDisplayName =
                get(documentRef = profileRef).data<ProfileEntity?>()?.displayName

            val nameToRelease =
                currentDisplayName?.takeIf {
                    it.isNotBlank() && it.normalize() != newDisplayName.normalize()
                }

            if (nameToRelease != null) {
                delete(documentRef = displayNameReference(displayName = nameToRelease))
            }

            reserve(nameRef = newNameRef, userId = userId, displayName = newDisplayName)
            updateFields(documentRef = profileRef) { FireStoreKeys.DISPLAY_NAME to newDisplayName }
            DisplayNameResult.SUCCESS
        }
    }

    override suspend fun incrementStats(userId: String, delta: ProfileStats) {
        val increments = delta.toStatIncrements()
        if (increments.isEmpty()) return

        profilesCollection.document(documentPath = userId).updateFields {
            increments.forEach { (fieldPathSegments, amount) ->
                FieldPath(*fieldPathSegments.toTypedArray()) to FieldValue.increment(value = amount)
            }
        }
    }

    private fun displayNameReference(displayName: String): DocumentReference =
        displayNamesCollection.document(documentPath = displayName.normalize())
}

/** Dotted Firestore field path to the overall Gauntlets-won counter the leaderboard ranks by. */
private const val OVERALL_GAUNTLETS_WON_PATH =
    FireStoreKeys.STATS + "." + FireStoreKeys.OVERALL + "." + FireStoreKeys.GAUNTLETS_WON

/**
 * The key a display name is reserved under. Names are unique case-insensitively, with surrounding
 * whitespace and path separators (which Firestore would treat as subcollections) stripped.
 */
private fun String.normalize(): String = trim().lowercase().replace(oldChar = '/', newChar = '_')

private suspend fun Transaction.isReservedByOther(
    nameRef: DocumentReference,
    userId: String,
): Boolean {
    val snapshot = get(documentRef = nameRef)
    return snapshot.exists && snapshot.data<DisplayNameEntity>().userId != userId
}

private fun Transaction.reserve(nameRef: DocumentReference, userId: String, displayName: String) {
    set(documentRef = nameRef, data = DisplayNameEntity(userId = userId, displayName = displayName))
}
