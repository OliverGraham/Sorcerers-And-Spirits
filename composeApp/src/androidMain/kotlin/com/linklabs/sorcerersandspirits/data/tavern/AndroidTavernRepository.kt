package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.data.getChildrenFlow
import com.linklabs.sorcerersandspirits.data.getValueFlow
import com.linklabs.sorcerersandspirits.data.model.ChallengeEntity
import com.linklabs.sorcerersandspirits.data.model.ChallengerEntity
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.data.model.toDomain
import com.linklabs.sorcerersandspirits.data.model.toEntity
import com.linklabs.sorcerersandspirits.data.runTransactionAwait
import com.linklabs.sorcerersandspirits.domain.tavern.Cancelable
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AndroidTavernRepository : TavernRepository {

    private val database = FirebaseDatabase.getInstance()
    private val connectedReference = database.getReference(CONNECTED_REFERENCE_PATH)

    private val root = database.reference
    private val challengesRef = root.child(CHALLENGES_PATH)
    private val mailboxesRef = root.child(CHALLENGE_MAILBOXES_PATH)
    private val travelersReference = root.child(TAVERN_TRAVELERS_PATH)

    /**
     * Observes the user's mailbox for the current challengeId, then switches to that challenge's
     * updates. If the mailbox is empty, this emits `null`; if the mailbox changes, observation
     * moves to the newly referenced challenge.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveChallengeFlow(userId: String): Flow<Challenge?> =
        getMailboxChallengeIdFlow(userId).distinctUntilChanged().transformLatest { challengeId ->
            if (challengeId.isNullOrBlank()) {
                emit(null)
            } else {
                emitAll(flow = getChallengeFlow(challengeId = challengeId))
            }
        }

    override fun getTavernTravelersFlow(): Flow<List<Traveler>> =
        travelersReference.getChildrenFlow<TravelerEntity>().map { travelerEntities ->
            travelerEntities.map { travelerEntity -> travelerEntity.toDomain() }
        }

    override fun connectTravelerPresence(userId: String, displayName: String): Cancelable {
        val travelerRef = travelersReference.child(userId)
        val mailboxRef = mailboxesRef.child(userId)

        val connectTravelerPresenceListener =
            ConnectTravelerPresenceListener(
                userId = userId,
                displayName = displayName,
                travelerReference = travelerRef,
                mailBoxReference = mailboxRef,
            )

        connectedReference.addValueEventListener(connectTravelerPresenceListener)

        return Cancelable {
            connectedReference.removeEventListener(connectTravelerPresenceListener)
            travelerRef.onDisconnect().cancel()
        }
    }

    private data class ClaimChallengerResult(val isClaimed: Boolean, val challenger: Challenger)

    override suspend fun sendChallenge(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult {
        val challengeId =
            challengesRef.push().key
                ?: return SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)

        val (claimedChallengers, unclaimedChallengers) =
            claimChallengers(challengers = (listOf(instigator) + invitees))

        if (claimedChallengers.size < MINIMUM_CHALLENGERS) {
            releaseClaims(userIds = claimedChallengers.map { it.challenger.userId })
            return SendChallengeResult.NotEnoughChallengersAvailable(
                unavailableChallengers = unclaimedChallengers.map { it.challenger }
            )
        }

        val challengeEntity =
            ChallengeEntity(
                challengeId = challengeId,
                challengerEntitiesMap =
                    claimedChallengers.associate { result ->
                        val challenger = result.challenger
                        challenger.userId to challenger.toEntity()
                    },
            )

        return try {
            root
                .updateChildren(
                    buildMap {
                        put(key = challengeId.toChallengesPath(), value = challengeEntity)
                        claimedChallengers.forEach {
                            put(
                                key = it.challenger.userId.toChallengeMailBoxesPath(),
                                value = challengeId,
                            )
                        }
                    }
                )
                .await()

            SendChallengeResult.Success(challengeId = challengeId)
        } catch (_: Exception) {
            releaseClaims(userIds = claimedChallengers.map { it.challenger.userId })
            SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)
        }
    }

    private suspend fun claimChallengers(challengers: List<Challenger>) =
        withContext(context = Dispatchers.IO) {
            coroutineScope {
                    challengers
                        .map { challenger ->
                            async {
                                ClaimChallengerResult(
                                    isClaimed = tryClaimTraveler(userId = challenger.userId),
                                    challenger = challenger,
                                )
                            }
                        }
                        .awaitAll()
                }
                .partition { it.isClaimed }
        }

    override suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        challengesRef
            .child(challengeId)
            .child(ChallengeEntity.challengerEntitiesMapName)
            .child(userId)
            .child(ChallengerEntity.statusName)
            .setValue(status)
            .await()

        if (status == ChallengerStatus.Declined) {
            mailboxesRef.child(userId).removeValue().await()
            setTravelerAvailableIfPresent(userId = userId)
        }

        reconcileChallenge(challengeId = challengeId)
    }

    private fun getMailboxChallengeIdFlow(userId: String): Flow<String?> =
        mailboxesRef.child(userId).getValueFlow<String>()

    private fun getChallengeFlow(challengeId: String): Flow<Challenge?> =
        challengesRef.child(challengeId).getValueFlow<ChallengeEntity>().map { challengeEntity ->
            challengeEntity?.toDomain()
        }

    private suspend fun reconcileChallenge(challengeId: String) {
        val challengeEntity =
            challengesRef.child(challengeId).get().await().getValue<ChallengeEntity>() ?: return

        val challengerEntitiesMap = challengeEntity.challengerEntitiesMap
        val participantUserIds = challengerEntitiesMap.keys.toList()

        val nonDeclinedStatuses =
            challengerEntitiesMap.values
                .map { challengerEntity -> challengerEntity.status }
                .filterNot { status -> status == ChallengerStatus.Declined }

        val hasEveryoneAccepted =
            nonDeclinedStatuses.isNotEmpty() &&
                nonDeclinedStatuses.all { status -> status == ChallengerStatus.Accepted }

        val doesNotHaveEnoughPlayersLeft = nonDeclinedStatuses.size < MINIMUM_CHALLENGERS

        when {
            hasEveryoneAccepted -> {
                clearChallengeData(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
            }

            doesNotHaveEnoughPlayersLeft -> {
                clearChallengeData(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
                releaseClaims(userIds = participantUserIds)
            }
        }
    }

    private suspend fun tryClaimTraveler(userId: String): Boolean =
        travelersReference.child(userId).runTransactionAwait { currentData ->
            val traveler =
                currentData.getValue<TravelerEntity>()
                    ?: return@runTransactionAwait Transaction.abort()

            if (traveler.tavernStatus != TavernStatus.Available) {
                return@runTransactionAwait Transaction.abort()
            }

            currentData.value = traveler.copy(tavernStatus = TavernStatus.Busy)

            Transaction.success(currentData)
        }

    override suspend fun setTravelerAvailableIfPresent(userId: String) {
        travelersReference.child(userId).runTransactionAwait { currentData ->
            val traveler =
                currentData.getValue<TravelerEntity>()
                    ?: return@runTransactionAwait Transaction.success(currentData)

            currentData.value = traveler.copy(tavernStatus = TavernStatus.Available)

            Transaction.success(currentData)
        }
    }

    private suspend fun releaseClaims(userIds: List<String>) {
        userIds.forEach { userId -> setTravelerAvailableIfPresent(userId = userId) }
    }

    private suspend fun clearChallengeData(challengeId: String, participantUserIds: List<String>) {
        val updates =
            buildMap<String, Any?> {
                put(key = challengeId.toChallengesPath(), value = null)

                participantUserIds.forEach { userId ->
                    put(key = userId.toChallengeMailBoxesPath(), value = null)
                }
            }

        root.updateChildren(updates).await()
    }

    private companion object {
        private const val MINIMUM_CHALLENGERS = 2
        private const val CONNECTED_REFERENCE_PATH = ".info/connected"
        private const val SEND_RESULT_FAILURE_MESSAGE = "Something went wrong, please try again."

        private fun String.toChallengesPath() = "$CHALLENGES_PATH/$this"

        private fun String.toChallengeMailBoxesPath() = "$CHALLENGE_MAILBOXES_PATH/$this"
    }
}
