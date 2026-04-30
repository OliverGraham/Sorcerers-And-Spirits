package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.TAVERN_TRAVELERS_PATH
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlin.collections.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

class FirebaseTavernRepository(
    database: FirebaseDatabase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : TavernRepository {

    private val connectedReference = database.reference(CONNECTED_REFERENCE_PATH)

    private val root = database.reference()
    private val tavernTravelersReference = root.child(TAVERN_TRAVELERS_PATH)
    private val mailboxChallengesReference = root.child(CHALLENGE_MAILBOXES_PATH)
    private val challengesReference = root.child(CHALLENGES_PATH)

    private var travelerPresenceJob: Job? = null

    /**
     * Observes the user's mailbox for the current challengeId, then switches to that challenge's
     * updates. If the mailbox is empty, this emits `null`; if the mailbox changes, observation
     * moves to the newly referenced challenge.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveChallengeFlow(userId: String): Flow<Challenge?> =
        getMailboxChallengeIdFlow(userId).distinctUntilChanged().transformLatest { challengeId ->
            if (challengeId.isNullOrBlank()) {
                emit(value = null)
            } else {
                emitAll(flow = getChallengeFlow(challengeId = challengeId))
            }
        }

    override fun getTavernTravelersFlow(): Flow<List<Traveler>> =
        tavernTravelersReference.valueEvents
            .map { snapshot ->
                snapshot.children.mapNotNull { child ->
                    child.value(TravelerEntity.serializer().nullable)?.toDomain()
                }
            }
            .distinctUntilChanged()

    override suspend fun connectTravelerPresence(userId: String, displayName: String) {
        travelerPresenceJob?.cancel()

        travelerPresenceJob =
            connectedReference.valueEvents
                .map { snapshot -> snapshot.value<Boolean?>() ?: false }
                .distinctUntilChanged()
                .filter { isConnected -> isConnected }
                .onEach {
                    val travelerReference = tavernTravelersReference.child(userId)
                    travelerReference.onDisconnect().removeValue()
                    syncTravelerPresenceOnConnect(
                        userId = userId,
                        displayName = displayName,
                        travelerReference = travelerReference,
                    )
                }
                .launchIn(scope = coroutineScope)
    }

    override suspend fun sendChallenge(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult {
        val challengeId =
            challengesReference.push().key
                ?: return SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)

        val (claimedChallengers, unclaimedChallengers) =
            claimChallengers(challengers = listOf(instigator) + invitees)

        if (claimedChallengers.size < MINIMUM_CHALLENGERS) {
            releaseTravelersIfMailboxMatches(
                userIds = claimedChallengers.map { it.challenger.userId },
                challengeId = challengeId,
            )
            return SendChallengeResult.NotEnoughChallengersAvailable(
                unavailableChallengers = unclaimedChallengers.map { it.challenger }
            )
        }

        val challengeEntity =
            createChallengeEntity(
                challengeId = challengeId,
                claimedChallengers = claimedChallengers,
            )

        return try {
            applyUpdates {
                putChallenge(challengeEntity = challengeEntity)
                setMailboxes(
                    userIds = claimedChallengers.map { it.challenger.userId },
                    challengeId = challengeId,
                )
            }

            SendChallengeResult.Success(challengeId = challengeId)
        } catch (_: Exception) {
            releaseTravelersIfMailboxMatches(
                userIds = claimedChallengers.map { it.challenger.userId },
                challengeId = challengeId,
            )
            SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)
        }
    }

    override suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) {
        challengesReference
            .child(challengeId)
            .child(ChallengeEntity.challengerEntitiesMapName)
            .child(userId)
            .child(ChallengerEntity.statusName)
            .setValue(status)

        if (status == ChallengerStatus.Declined) {
            mailboxChallengesReference.child(userId).removeValue()
            releaseTravelerIfMailboxEmpty(userId = userId)
        }

        reconcileChallengeState(challengeId = challengeId)
    }

    override suspend fun setTravelerAvailableIfPresent(userId: String) {
        updateTravelerStatusIfPresent(userId = userId, tavernStatus = TavernStatus.Available)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncTravelerPresenceOnConnect(
        userId: String,
        displayName: String,
        travelerReference: DatabaseReference,
    ) {
        val mailboxReference = mailboxChallengesReference.child(userId)

        suspend fun upsertPresence(hasActiveChallenge: Boolean) {
            travelerReference.runTransaction(strategy = TravelerEntity.serializer().nullable) {
                currentTraveler ->
                val tavernStatus =
                    if (hasActiveChallenge) TavernStatus.Busy else TavernStatus.Available

                currentTraveler?.copy(
                    userId = userId,
                    displayName = displayName,
                    tavernStatus = tavernStatus,
                )
                    ?: TravelerEntity(
                        userId = userId,
                        displayName = displayName,
                        tavernStatus = tavernStatus,
                    )
            }
        }

        val challengeId = mailboxReference.awaitNullableValue<String>()
        if (challengeId.isNullOrBlank()) {
            upsertPresence(hasActiveChallenge = false)
            return
        }

        val challengeEntity =
            challengesReference.child(challengeId).awaitNullableValue<ChallengeEntity>()

        when {
            challengeEntity == null -> {
                mailboxReference.removeValue()
                upsertPresence(hasActiveChallenge = false)
            }

            challengeEntity.isExpired() -> {
                expireChallenge(
                    challengeId = challengeId,
                    participantUserIds = challengeEntity.challengerEntitiesMap.keys.toList(),
                )
                upsertPresence(hasActiveChallenge = false)
            }

            else -> upsertPresence(hasActiveChallenge = true)
        }
    }

    private suspend fun claimChallengers(
        challengers: List<Challenger>
    ): Pair<List<ClaimChallengerResult>, List<ClaimChallengerResult>> = coroutineScope {
        challengers
            .map { challenger ->
                async(context = Dispatchers.IO) {
                    ClaimChallengerResult(
                        isClaimed = tryMarkTravelerBusy(userId = challenger.userId),
                        challenger = challenger,
                    )
                }
            }
            .awaitAll()
            .partition { it.isClaimed }
    }

    private suspend fun tryMarkTravelerBusy(userId: String): Boolean =
        try {
            tavernTravelersReference.child(userId).runTransaction(
                strategy = TravelerEntity.serializer().nullable
            ) { traveler ->
                traveler ?: throw AbortTransactionException()

                if (traveler.tavernStatus != TavernStatus.Available) {
                    throw AbortTransactionException()
                }

                traveler.copy(tavernStatus = TavernStatus.Busy)
            }

            true
        } catch (_: AbortTransactionException) {
            false
        }

    private suspend fun updateTravelerStatusIfPresent(userId: String, tavernStatus: TavernStatus) {
        tavernTravelersReference.child(userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable
        ) { traveler ->
            traveler?.copy(tavernStatus = tavernStatus) ?: traveler
        }
    }

    private suspend fun releaseTravelersIfMailboxMatches(
        userIds: List<String>,
        challengeId: String,
    ) {
        userIds.forEach { userId ->
            releaseTravelerIfMailboxMatches(userId = userId, challengeId = challengeId)
        }
    }

    private suspend fun releaseTravelerIfMailboxMatches(userId: String, challengeId: String) {
        val mailboxChallengeId =
            mailboxChallengesReference.child(userId).awaitNullableValue<String>()

        if (mailboxChallengeId != null && mailboxChallengeId != challengeId) {
            return
        }

        tavernTravelersReference.child(userId).runTransaction(
            strategy = TravelerEntity.serializer().nullable
        ) { traveler ->
            when {
                traveler == null -> null
                traveler.tavernStatus != TavernStatus.Busy -> traveler
                else -> traveler.copy(tavernStatus = TavernStatus.Available)
            }
        }
    }

    private suspend fun releaseTravelerIfMailboxEmpty(userId: String) {
        val mailboxChallengeId =
            mailboxChallengesReference.child(userId).awaitNullableValue<String>()

        if (!mailboxChallengeId.isNullOrBlank()) {
            return
        }

        tavernTravelersReference.child(userId).runTransaction(
            TravelerEntity.serializer().nullable
        ) { traveler ->
            when {
                traveler == null -> null
                traveler.tavernStatus != TavernStatus.Busy -> traveler
                else -> traveler.copy(tavernStatus = TavernStatus.Available)
            }
        }
    }

    private fun getMailboxChallengeIdFlow(userId: String): Flow<String?> =
        mailboxChallengesReference
            .child(userId)
            .valueEvents
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.value(strategy = String.serializer().nullable)
                } else {
                    null
                }
            }
            .distinctUntilChanged()

    private fun getChallengeFlow(challengeId: String): Flow<Challenge?> =
        challengesReference
            .child(challengeId)
            .valueEvents
            .map { snapshot ->
                snapshot.value(strategy = ChallengeEntity.serializer().nullable)?.toDomain()
            }
            .distinctUntilChanged()

    private suspend fun reconcileChallengeState(challengeId: String) {
        val challengeEntity =
            challengesReference.child(challengeId).awaitNullableValue<ChallengeEntity>() ?: return

        val participantUserIds = challengeEntity.challengerEntitiesMap.keys.toList()

        when (resolveChallengeState(challengeEntity = challengeEntity)) {
            ChallengeResolution.None -> Unit

            ChallengeResolution.Complete -> {
                removeChallengeReferences(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
            }

            ChallengeResolution.CancelInsufficientPlayers -> {
                removeChallengeReferences(
                    challengeId = challengeId,
                    participantUserIds = participantUserIds,
                )
                releaseTravelersIfMailboxMatches(
                    userIds = participantUserIds,
                    challengeId = challengeId,
                )
            }
        }
    }

    private fun resolveChallengeState(challengeEntity: ChallengeEntity): ChallengeResolution {
        val nonDeclinedStatuses =
            challengeEntity.challengerEntitiesMap.values
                .map { it.status }
                .filterNot { it == ChallengerStatus.Declined }

        val doesNotHaveEnoughPlayersLeft = nonDeclinedStatuses.size < MINIMUM_CHALLENGERS
        if (doesNotHaveEnoughPlayersLeft) return ChallengeResolution.CancelInsufficientPlayers

        val hasEveryoneAccepted = nonDeclinedStatuses.all { it == ChallengerStatus.Accepted }

        return if (hasEveryoneAccepted) ChallengeResolution.Complete else ChallengeResolution.None
    }

    private suspend fun expireChallenge(challengeId: String, participantUserIds: List<String>) {
        removeChallengeReferences(
            challengeId = challengeId,
            participantUserIds = participantUserIds,
        )
        releaseTravelersIfMailboxMatches(userIds = participantUserIds, challengeId = challengeId)
    }

    @OptIn(ExperimentalTime::class)
    private fun createChallengeEntity(
        challengeId: String,
        claimedChallengers: List<ClaimChallengerResult>,
    ): ChallengeEntity {
        val startTimeMillis = Clock.System.now().toEpochMilliseconds()
        val endTimeMillis = startTimeMillis + 1.minutes.inWholeMilliseconds

        return ChallengeEntity(
            challengeId = challengeId,
            challengerEntitiesMap =
                claimedChallengers.associate { result ->
                    result.challenger.userId to result.challenger.toEntity()
                },
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
        )
    }

    private suspend fun removeChallengeReferences(
        challengeId: String,
        participantUserIds: List<String>,
    ) {
        applyUpdates {
            removeChallenge(challengeId)
            clearMailboxes(participantUserIds)
        }
    }

    private suspend fun applyUpdates(builder: MutableMap<String, Any?>.() -> Unit) {
        root.updateChildren(update = buildMap(builderAction = builder))
    }

    private data class ClaimChallengerResult(val isClaimed: Boolean, val challenger: Challenger)

    private enum class ChallengeResolution {
        None,
        Complete,
        CancelInsufficientPlayers,
    }

    private class AbortTransactionException : RuntimeException()

    companion object {
        const val CONNECTED_REFERENCE_PATH = ".info/connected"
        const val MINIMUM_CHALLENGERS = 2
        const val SEND_RESULT_FAILURE_MESSAGE = "Something went wrong, please try again."
    }
}

// TODO: Move to separate file

private fun MutableMap<String, Any?>.putChallenge(challengeEntity: ChallengeEntity) {
    this[challengeEntity.challengeId.toChallengesPath()] = challengeEntity
}

private fun MutableMap<String, Any?>.removeChallenge(challengeId: String) {
    this[challengeId.toChallengesPath()] = null
}

private fun MutableMap<String, Any?>.setMailbox(userId: String, challengeId: String) {
    this[userId.toChallengeMailBoxesPath()] = challengeId
}

private fun MutableMap<String, Any?>.clearMailbox(userId: String) {
    this[userId.toChallengeMailBoxesPath()] = null
}

private fun MutableMap<String, Any?>.setMailboxes(userIds: List<String>, challengeId: String) {
    userIds.forEach { userId -> setMailbox(userId = userId, challengeId = challengeId) }
}

private fun MutableMap<String, Any?>.clearMailboxes(userIds: List<String>) {
    userIds.forEach(action = ::clearMailbox)
}

@OptIn(ExperimentalTime::class)
private fun ChallengeEntity.isExpired(): Boolean =
    endTimeMillis?.let { it <= Clock.System.now().toEpochMilliseconds() } == true

internal fun String.toChallengesPath(): String = "$CHALLENGES_PATH/$this"

internal fun String.toChallengeMailBoxesPath(): String = "$CHALLENGE_MAILBOXES_PATH/$this"

private suspend inline fun <reified T> DatabaseReference.awaitNullableValue(): T? {
    val snapshot = valueEvents.first()

    return if (snapshot.exists) {
        snapshot.value<T>()
    } else {
        null
    }
}
