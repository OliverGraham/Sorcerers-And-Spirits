package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.flow.Flow

interface TavernRepository {

    fun getActiveChallengeFlow(userId: String): Flow<Challenge?>

    fun getTavernTravelersFlow(): Flow<List<Traveler>>

    suspend fun connectTravelerPresence(userId: String, displayName: String)

    suspend fun sendChallenge(
        instigator: Challenger,
        invitees: List<Challenger>,
    ): SendChallengeResult

    suspend fun updateChallengerStatus(
        challengeId: String,
        userId: String,
        status: ChallengerStatus,
    )

    suspend fun setTravelerAvailableIfPresent(userId: String)
}

sealed interface SendChallengeResult {
    data class Success(val challengeId: String) : SendChallengeResult

    data class Failure(val errorMessage: String) : SendChallengeResult

    data class NotEnoughChallengersAvailable(val unavailableChallengers: List<Challenger>) :
        SendChallengeResult
}
