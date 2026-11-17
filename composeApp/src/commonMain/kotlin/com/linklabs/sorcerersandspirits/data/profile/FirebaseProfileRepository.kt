package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.authentication.SessionStore
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

class FirebaseProfileRepository(
    private val database: FirebaseFirestore,
    private val sessionStore: SessionStore,
) : ProfileRepository {

    private val profilesCollection = database.collection(collectionPath = FireStoreKeys.PROFILES)
    private val displayNamesCollection =
        database.collection(collectionPath = FireStoreKeys.DISPLAY_NAMES)

    override suspend fun createProfile(profile: Profile): DisplayNameResult = try {
        val profileRef = profilesCollection.document(documentPath = profile.userId)
        val nameRef = displayNameReference(displayName = profile.displayName)

        database.runTransaction {
            if (isReservedByOther(nameRef = nameRef, userId = profile.userId)) {
                return@runTransaction DisplayNameResult.NAME_TAKEN
            }

            set(documentRef = profileRef, data = profile.toEntity())
            reserve(nameRef = nameRef, userId = profile.userId, displayName = profile.displayName)
            DisplayNameResult.SUCCESS
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        DisplayNameResult.ERROR
    }

    override suspend fun deleteProfile(userId: String) = swallowFirebaseErrors {
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

        sessionStore.clear()
    }

    override suspend fun getProfile(userId: String): Profile? {
        val remoteProfile = fetchRemoteProfile(userId = userId)
        if (remoteProfile != null) {
            cacheSession(profile = remoteProfile)
            return remoteProfile
        }

        return cachedProfileFor(userId = userId)
    }

    override suspend fun getTopProfiles(limit: Int): List<Profile> = try {
        profilesCollection
            .orderBy(field = OVERALL_GAUNTLETS_WON_PATH, direction = Direction.DESCENDING)
            .limit(limit = limit)
            .get()
            .documents
            .mapNotNull { document -> document.data<ProfileEntity?>()?.toDomain() }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        emptyList()
    }

    override fun getProfileFlow(userId: String): Flow<Profile?> = profilesCollection.document(documentPath = userId).snapshots.map { snapshot ->
        snapshot.data<ProfileEntity?>()?.toDomain()
    }.onEach { profile ->
        if (profile != null) {
            cacheSession(profile = profile)
        }
    }.onStart {
        val cachedProfile = cachedProfileFor(userId = userId)
        if (cachedProfile != null) {
            emit(cachedProfile)
        }
    }.catch {
        val cachedProfile = cachedProfileFor(userId = userId)
        if (cachedProfile != null) {
            emit(cachedProfile)
        }
    }

    override suspend fun updateDisplayName(
        userId: String,
        newDisplayName: String,
    ): DisplayNameResult {
        val profileRef = profilesCollection.document(documentPath = userId)
        val newNameRef = displayNameReference(displayName = newDisplayName)

        return try {
            database.runTransaction {
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            DisplayNameResult.ERROR
        }
    }

    override suspend fun incrementStats(userId: String, delta: ProfileStats) {
        val increments = delta.toStatIncrements()
        if (increments.isEmpty()) return

        swallowFirebaseErrors {
            profilesCollection.document(documentPath = userId).updateFields {
                increments.forEach { (fieldPathSegments, amount) ->
                    FieldPath(*fieldPathSegments.toTypedArray()) to FieldValue.increment(value = amount)
                }
            }
        }
    }

    override suspend fun unlockAchievements(
        userId: String,
        achievementIds: List<AchievementId>,
        unlockedAt: Long,
    ) {
        if (achievementIds.isEmpty()) return

        swallowFirebaseErrors {
            profilesCollection.document(documentPath = userId).updateFields {
                achievementIds.forEach { achievementId ->
                    FieldPath(FireStoreKeys.UNLOCKED_ACHIEVEMENTS, achievementId.name) to unlockedAt
                }
            }
        }
    }

    private suspend fun swallowFirebaseErrors(write: suspend () -> Unit) {
        try {
            write()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
        }
    }

    private fun displayNameReference(displayName: String): DocumentReference = displayNamesCollection.document(documentPath = displayName.normalize())

    private suspend fun fetchRemoteProfile(userId: String): Profile? = try {
        withTimeoutOrNull(timeMillis = REMOTE_PROFILE_TIMEOUT_MILLIS) {
            profilesCollection.document(documentPath = userId).get()
                .data<ProfileEntity?>()
                ?.toDomain()
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private fun cachedProfileFor(userId: String): Profile? {
        val cachedSession = sessionStore.cached() ?: return null
        if (cachedSession.userId != userId) return null

        return Profile(userId = cachedSession.userId, displayName = cachedSession.displayName)
    }

    private suspend fun cacheSession(profile: Profile) {
        sessionStore.save(userId = profile.userId, displayName = profile.displayName)
    }
}

private const val REMOTE_PROFILE_TIMEOUT_MILLIS = 5_000L

private const val OVERALL_GAUNTLETS_WON_PATH =
    FireStoreKeys.STATS + "." + FireStoreKeys.OVERALL + "." + FireStoreKeys.GAUNTLETS_WON

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
