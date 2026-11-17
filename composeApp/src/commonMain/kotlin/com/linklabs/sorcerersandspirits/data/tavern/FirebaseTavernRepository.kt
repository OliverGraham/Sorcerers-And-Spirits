package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.SendChallengeResult
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime

class FirebaseTavernRepository(
    private val dataSource: TavernDataSource,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : TavernRepository {

    private var travelerPresenceJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveChallengeFlow(userId: String): Flow<Challenge?> = dataSource.mailboxChallengeIdFlow(
        userId = userId,
    ).distinctUntilChanged().transformLatest { challengeId ->
        if (challengeId.isNullOrBlank()) {
            emit(value = null)
        } else {
            emitAll(flow = getChallengeFlow(challengeId = challengeId))
        }
    }

    override fun getTavernTravelersFlow(): Flow<List<Traveler>> = dataSource
        .travelerEntitiesFlow()
        .map { entities -> entities.map(transform = TravelerEntity::toDomain) }
        .distinctUntilChanged()

    override suspend fun connectTravelerPresence(
        userId: String,
        displayName: String,
        rating: Long,
    ) {
        travelerPresenceJob?.cancel()

        travelerPresenceJob =
            dataSource
                .connectedFlow()
                .distinctUntilChanged()
                .filter { isConnected -> isConnected }
                .onEach {
                    try {
                        dataSource.registerTravelerRemovalOnDisconnect(userId = userId)
                        syncTravelerPresenceOnConnect(
                            userId = userId,
                            displayName = displayName,
                            rating = rating,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                    }
                }
                .launchIn(scope = coroutineScope)
    }

    override suspend fun sendChallenge(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult = try {
        sendChallengeOrThrow(instigator = instigator, invitees = invitees)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)
    }

    private suspend fun sendChallengeOrThrow(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult {
        val challengeId =
            dataSource.generateChallengeId()
                ?: return SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)

        val (claimedChallengers, unclaimedChallengers) =
            claimChallengers(challengers = listOf(instigator) + invitees)

        if (claimedChallengers.size < MINIMUM_CHALLENGERS) {
            releaseTravelersIfMailboxMatches(
                userIds = claimedChallengers.map { it.challenger.userId },
                challengeId = challengeId,
            )
            return SendChallengeResult.NotEnoughChallengersAvailable(
                unavailableChallengers = unclaimedChallengers.map { it.challenger },
            )
        }

        val claimedUserIds = claimedChallengers.map { it.challenger.userId }
        val challengers =
            claimedChallengers.associate { result ->
                result.challenger.userId to result.challenger.toEntity()
            }

        return try {
            dataSource.createChallenge(
                challengeId = challengeId,
                challengers = challengers,
                participantUserIds = claimedUserIds,
            )

            SendChallengeResult.Success(challengeId = challengeId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            releaseTravelersIfMailboxMatches(userIds = claimedUserIds, challengeId = challengeId)
            SendChallengeResult.Failure(errorMessage = SEND_RESULT_FAILURE_MESSAGE)
        }
    }

    override suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    ) = swallowFirebaseErrors {
        dataSource.setChallengerStatus(challengeId = challengeId, userId = userId, status = status)

        if (status == ChallengerStatus.Declined) {
            dataSource.removeMailbox(userId = userId)
            releaseTravelerIfMailboxEmpty(userId = userId)
        }

        reconcileChallengeState(challengeId = challengeId)
    }

    override suspend fun setTravelerAvailableIfPresent(userId: String) = swallowFirebaseErrors {
        dataSource.setTravelerStatusIfPresent(
            userId = userId,
            tavernStatus = TavernStatus.Available,
        )
    }

    private suspend fun swallowFirebaseErrors(write: suspend () -> Unit) {
        try {
            write()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
        }
    }

    private suspend fun syncTravelerPresenceOnConnect(
        userId: String,
        displayName: String,
        rating: Long,
    ) {
        val challengeId = dataSource.getMailboxChallengeId(userId = userId)
        if (challengeId.isNullOrBlank()) {
            upsertPresence(
                userId = userId,
                displayName = displayName,
                rating = rating,
                hasActiveChallenge = false,
            )
            return
        }

        val challengeEntity = dataSource.getChallengeEntity(challengeId = challengeId)

        when {
            challengeEntity == null -> {
                dataSource.removeMailbox(userId = userId)
                upsertPresence(
                    userId = userId,
                    displayName = displayName,
                    rating = rating,
                    hasActiveChallenge = false,
                )
            }

            challengeEntity.isExpired() -> {
                expireChallenge(
                    challengeId = challengeId,
                    participantUserIds = challengeEntity.challengerEntitiesMap.keys.toList(),
                )
                upsertPresence(
                    userId = userId,
                    displayName = displayName,
                    rating = rating,
                    hasActiveChallenge = false,
                )
            }

            else ->
                upsertPresence(
                    userId = userId,
                    displayName = displayName,
                    rating = rating,
                    hasActiveChallenge = true,
                )
        }
    }

    private suspend fun upsertPresence(
        userId: String,
        displayName: String,
        rating: Long,
        hasActiveChallenge: Boolean,
    ) {
        dataSource.upsertTravelerPresence(
            userId = userId,
            displayName = displayName,
            tavernStatus = if (hasActiveChallenge) TavernStatus.Busy else TavernStatus.Available,
            rating = rating,
        )
    }

    private suspend fun claimChallengers(
        challengers: List<Challenger>,
    ): Pair<List<ClaimChallengerResult>, List<ClaimChallengerResult>> = coroutineScope {
        challengers
            .map { challenger ->
                async(context = Dispatchers.IO) {
                    ClaimChallengerResult(
                        isClaimed =
                        dataSource.markTravelerBusyIfAvailable(userId = challenger.userId),
                        challenger = challenger,
                    )
                }
            }
            .awaitAll()
            .partition { it.isClaimed }
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
        val mailboxChallengeId = dataSource.getMailboxChallengeId(userId = userId)

        if (mailboxChallengeId != null && mailboxChallengeId != challengeId) {
            return
        }

        dataSource.releaseTravelerIfBusy(userId = userId)
    }

    private suspend fun releaseTravelerIfMailboxEmpty(userId: String) {
        val mailboxChallengeId = dataSource.getMailboxChallengeId(userId = userId)

        if (!mailboxChallengeId.isNullOrBlank()) {
            return
        }

        dataSource.releaseTravelerIfBusy(userId = userId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getChallengeFlow(challengeId: String): Flow<Challenge?> = dataSource
        .challengeEntityFlow(challengeId = challengeId)
        .map { entity -> entity?.toDomain() }
        .distinctUntilChanged()

    private suspend fun reconcileChallengeState(challengeId: String) {
        val challengeEntity = dataSource.getChallengeEntity(challengeId = challengeId) ?: return

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
                .map { it.status.toDomain() }
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

    private suspend fun removeChallengeReferences(
        challengeId: String,
        participantUserIds: List<String>,
    ) {
        dataSource.applyUpdates(
            updates =
            buildMap {
                removeChallenge(challengeId)
                clearMailboxes(participantUserIds)
            },
        )
    }

    private data class ClaimChallengerResult(val isClaimed: Boolean, val challenger: Challenger)

    private enum class ChallengeResolution {
        None,
        Complete,
        CancelInsufficientPlayers,
    }

    companion object {
        const val MINIMUM_CHALLENGERS = 2
        const val SEND_RESULT_FAILURE_MESSAGE = "Something went wrong, please try again."
    }
}
