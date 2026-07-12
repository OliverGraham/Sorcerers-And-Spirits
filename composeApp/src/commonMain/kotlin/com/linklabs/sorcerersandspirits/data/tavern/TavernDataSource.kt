package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import kotlinx.coroutines.flow.Flow

interface TavernDataSource {

    fun travelerEntitiesFlow(): Flow<List<TravelerEntity>>

    fun mailboxChallengeIdFlow(userId: String): Flow<String?>

    fun challengeEntityFlow(challengeId: String): Flow<ChallengeEntity?>

    fun connectedFlow(): Flow<Boolean>

    suspend fun getMailboxChallengeId(userId: String): String?

    suspend fun getChallengeEntity(challengeId: String): ChallengeEntity?

    fun generateChallengeId(): String?

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

    suspend fun markTravelerBusyIfAvailable(userId: String): Boolean

    suspend fun setTravelerStatusIfPresent(userId: String, tavernStatus: TavernStatus)

    suspend fun releaseTravelerIfBusy(userId: String)

    suspend fun setChallengerStatus(challengeId: String, userId: String, status: ChallengerStatus)

    suspend fun removeMailbox(userId: String)

    suspend fun applyUpdates(updates: Map<String, Any?>)
}
