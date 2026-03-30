package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import kotlinx.coroutines.flow.Flow

interface TavernRepository {

    fun getActiveChallengeFlow(userId: String): Flow<Challenge?>

    fun getTavernTravelersFlow(): Flow<List<Traveler>>

    suspend fun connectTravelerPresence(userId: String, displayName: String): Cancelable

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

/**
 * Copy of [TavernRepository], excluding the Flow and suspend functions (which iOS can't handle).
 */
interface TavernRepositoryCopyForIOS {
    fun aTravelerEntersTavern(traveler: Traveler)
}

// TODO: figure out
fun interface Cancelable {
    fun cancel()
}

sealed interface SendChallengeResult {
    data class Success(val challengeId: String) : SendChallengeResult

    data class Failure(val errorMessage: String) : SendChallengeResult

    data class NotEnoughChallengersAvailable(val unavailableChallengers: List<Challenger>) :
        SendChallengeResult
}
