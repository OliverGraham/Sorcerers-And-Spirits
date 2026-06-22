package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import kotlinx.coroutines.flow.Flow

/**
 * The set of primitive persistence operations the Tavern needs, isolated from any specific backend.
 *
 * All challenge/traveler orchestration logic lives in [FirebaseTavernRepository]; this seam only
 * exposes the individual reads, writes, and transactions so the repository can be tested against a
 * fake without depending on where the data actually comes from.
 */
interface TavernDataSource {

    fun travelerEntitiesFlow(): Flow<List<TravelerEntity>>

    fun mailboxChallengeIdFlow(userId: String): Flow<String?>

    fun challengeEntityFlow(challengeId: String): Flow<ChallengeEntity?>

    fun connectedFlow(): Flow<Boolean>

    suspend fun getMailboxChallengeId(userId: String): String?

    suspend fun getChallengeEntity(challengeId: String): ChallengeEntity?

    fun generateChallengeId(): String?

    /**
     * Atomically creates the challenge and its participants' mailboxes in a single write, stamping
     * the start time with the backend's clock so every client shares the same deadline.
     */
    suspend fun createChallenge(
        challengeId: String,
        challengers: Map<String, ChallengerEntity>,
        participantUserIds: List<String>,
    )

    suspend fun registerTravelerRemovalOnDisconnect(userId: String)

    suspend fun upsertTravelerPresence(
        userId: String,
        displayName: String,
        tavernStatus: TavernStatus,
        rating: Long,
    )

    /**
     * Marks the traveler Busy only if they are currently Available. Returns whether the claim won.
     */
    suspend fun markTravelerBusyIfAvailable(userId: String): Boolean

    /** Sets the traveler's status if the traveler exists; no-op otherwise. */
    suspend fun setTravelerStatusIfPresent(userId: String, tavernStatus: TavernStatus)

    /**
     * Returns a Busy traveler to Available; leaves Available travelers and absent travelers alone.
     */
    suspend fun releaseTravelerIfBusy(userId: String)

    suspend fun setChallengerStatus(challengeId: String, userId: String, status: ChallengerStatus)

    suspend fun removeMailbox(userId: String)

    suspend fun applyUpdates(updates: Map<String, Any?>)
}
