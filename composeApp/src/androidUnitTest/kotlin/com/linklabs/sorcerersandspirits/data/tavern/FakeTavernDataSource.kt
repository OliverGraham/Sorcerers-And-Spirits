package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [TavernDataSource] used to drive [FirebaseTavernRepository] (and the use cases on top
 * of it) without a real backend. Reads and writes mutate the backing [MutableStateFlow]s so
 * observers see the same state the repository's transactions produce.
 */
class FakeTavernDataSource : TavernDataSource {

    val travelers = MutableStateFlow<Map<String, TravelerEntity>>(value = emptyMap())
    val mailboxes = MutableStateFlow<Map<String, String>>(value = emptyMap())
    val challenges = MutableStateFlow<Map<String, ChallengeEntity>>(value = emptyMap())
    val connected = MutableStateFlow(value = false)

    var challengeIdToGenerate: String? = GENERATED_CHALLENGE_ID
    var applyUpdatesError: Throwable? = null

    val appliedUpdates = mutableListOf<Map<String, Any?>>()
    val disconnectRegistrations = mutableListOf<String>()

    fun seedTraveler(traveler: TravelerEntity) {
        travelers.update { it + (traveler.userId to traveler) }
    }

    fun seedMailbox(userId: String, challengeId: String) {
        mailboxes.update { it + (userId to challengeId) }
    }

    fun seedChallenge(challenge: ChallengeEntity) {
        challenges.update { it + (challenge.challengeId to challenge) }
    }

    fun traveler(userId: String): TravelerEntity? = travelers.value[userId]

    fun mailbox(userId: String): String? = mailboxes.value[userId]

    fun challenge(challengeId: String): ChallengeEntity? = challenges.value[challengeId]

    override fun travelerEntitiesFlow(): Flow<List<TravelerEntity>> =
        travelers.map { it.values.toList() }

    override fun mailboxChallengeIdFlow(userId: String): Flow<String?> =
        mailboxes.map { it[userId] }

    override fun challengeEntityFlow(challengeId: String): Flow<ChallengeEntity?> =
        challenges.map { it[challengeId] }

    override fun connectedFlow(): Flow<Boolean> = connected

    override suspend fun getMailboxChallengeId(userId: String): String? = mailboxes.value[userId]

    override suspend fun getChallengeEntity(challengeId: String): ChallengeEntity? =
        challenges.value[challengeId]

    override fun generateChallengeId(): String? = challengeIdToGenerate

    override suspend fun registerTravelerRemovalOnDisconnect(userId: String) {
        disconnectRegistrations += userId
    }

    override suspend fun upsertTravelerPresence(
        userId: String,
        displayName: String,
        tavernStatus: TavernStatus,
    ) {
        travelers.update { current ->
            val updated =
                current[userId]?.copy(
                    userId = userId,
                    displayName = displayName,
                    tavernStatus = tavernStatus,
                )
                    ?: TravelerEntity(
                        userId = userId,
                        displayName = displayName,
                        tavernStatus = tavernStatus,
                    )
            current + (userId to updated)
        }
    }

    override suspend fun markTravelerBusyIfAvailable(userId: String): Boolean {
        val traveler = travelers.value[userId] ?: return false
        if (traveler.tavernStatus != TavernStatus.Available) return false

        travelers.update { it + (userId to traveler.copy(tavernStatus = TavernStatus.Busy)) }
        return true
    }

    override suspend fun setTravelerStatusIfPresent(userId: String, tavernStatus: TavernStatus) {
        travelers.update { current ->
            val traveler = current[userId] ?: return@update current
            current + (userId to traveler.copy(tavernStatus = tavernStatus))
        }
    }

    override suspend fun releaseTravelerIfBusy(userId: String) {
        travelers.update { current ->
            val traveler = current[userId] ?: return@update current
            if (traveler.tavernStatus != TavernStatus.Busy) {
                current
            } else {
                current + (userId to traveler.copy(tavernStatus = TavernStatus.Available))
            }
        }
    }

    override suspend fun setChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        challenges.update { current ->
            val challenge = current[challengeId] ?: return@update current
            val challenger = challenge.challengerEntitiesMap[userId] ?: return@update current
            val updatedMap =
                challenge.challengerEntitiesMap + (userId to challenger.copy(status = status))
            current + (challengeId to challenge.copy(challengerEntitiesMap = updatedMap))
        }
    }

    override suspend fun removeMailbox(userId: String) {
        mailboxes.update { it - userId }
    }

    override suspend fun applyUpdates(updates: Map<String, Any?>) {
        appliedUpdates += updates

        applyUpdatesError?.let { throw it }

        updates.forEach { (path, value) -> applyUpdate(path = path, value = value) }
    }

    private fun applyUpdate(path: String, value: Any?) {
        when {
            path.startsWith(prefix = CHALLENGES_PREFIX) -> {
                val challengeId = path.removePrefix(prefix = CHALLENGES_PREFIX)
                if (value == null) {
                    challenges.update { it - challengeId }
                } else {
                    challenges.update { it + (challengeId to value as ChallengeEntity) }
                }
            }

            path.startsWith(prefix = MAILBOXES_PREFIX) -> {
                val userId = path.removePrefix(prefix = MAILBOXES_PREFIX)
                if (value == null) {
                    mailboxes.update { it - userId }
                } else {
                    mailboxes.update { it + (userId to value as String) }
                }
            }
        }
    }

    companion object {
        const val GENERATED_CHALLENGE_ID = "generated-challenge"
        private const val CHALLENGES_PREFIX = "Challenges/"
        private const val MAILBOXES_PREFIX = "ChallengeMailboxes/"
    }
}
