package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

@Immutable
data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
    val challenge: ChallengeState? = null,
)

@Immutable
data class TravelerState(
    val userId: String,
    val displayName: String,
    val isThisTravelerTheUser: Boolean,
    val tavernStatus: TavernStatus,
)

@Immutable
data class ChallengerState(
    val userId: String,
    val displayName: String,
    val isThisChallengerTheUser: Boolean,
    val isInstigator: Boolean,
    val status: ChallengerStatus,
)

@Immutable
data class ChallengeState(
    val challengeId: String,
    val challengerStates: List<ChallengerState>,
    val showChallengeDialog: Boolean = false,
    val secondsRemaining: Int? = null,
) {
    val sender: ChallengerState? =
        challengerStates.find { challengerState -> challengerState.isInstigator }
}

internal fun List<Traveler>.toTavernState(userId: String?): TavernState? {
    val (userTravelers, otherTravelers) = partition { traveler -> traveler.userId == userId }

    val userTraveler =
        userTravelers.firstOrNull()?.toTravelerState(isThisTravelerTheUser = true) ?: return null

    return TavernState(
        userTraveler = userTraveler,
        otherTravelers =
            otherTravelers.map { traveler ->
                traveler.toTravelerState(isThisTravelerTheUser = false)
            },
    )
}

private fun Traveler.toTravelerState(isThisTravelerTheUser: Boolean): TravelerState =
    TravelerState(
        userId = userId,
        displayName = displayName,
        isThisTravelerTheUser = isThisTravelerTheUser,
        tavernStatus = tavernStatus,
    )

internal fun TravelerState.toChallenger(isInstigator: Boolean, status: ChallengerStatus) =
    Challenger(
        userId = userId,
        displayName = displayName,
        isInstigator = isInstigator,
        status = status,
    )

internal fun Challenge.toChallengeState(
    showChallengeDialog: Boolean,
    secondsRemaining: Int? = null,
) =
    ChallengeState(
        challengeId = challengeId,
        challengerStates = challengers.values.map { challenger -> challenger.toChallengerState() },
        showChallengeDialog = showChallengeDialog,
        secondsRemaining = secondsRemaining,
    )

private fun Challenger.toChallengerState() =
    ChallengerState(
        userId = userId,
        displayName = displayName,
        isThisChallengerTheUser = false,
        isInstigator = isInstigator,
        status = status,
    )
