package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

@Immutable
data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
    val challenge: ChallengeState?,
)

@Immutable
data class TravelerState(
    val userId: String,
    val displayName: String,
    val isThisTravelerTheUser: Boolean,
    val tavernStatus: TavernStatus,
)

@Immutable
data class ChallengeState(
    val challengeId: String,
    val challengerStates: List<ChallengerState>,
    val showChallengeDialog: Boolean = false,
    val secondsRemaining: Int? = null,
    val navigateToArena: Boolean = false,
) {
    val sender: ChallengerState? =
        challengerStates.find { challengerState -> challengerState.isInstigator }
}

@Immutable
data class ChallengerState(
    val userId: String,
    val displayName: String,
    val isInstigator: Boolean,
    val status: ChallengerStatus,
)

internal fun TavernData.toState(challengeState: ChallengeState?): TavernState? {
    val (userTravelers, otherTravelers) =
        travelers.partition { traveler -> traveler.userId == profile.userId }

    val userTraveler =
        userTravelers.firstOrNull()?.toTravelerState(isThisTravelerTheUser = true) ?: return null

    return TavernState(
        userTraveler = userTraveler,
        otherTravelers =
            otherTravelers.map { traveler ->
                traveler.toTravelerState(isThisTravelerTheUser = false)
            },
        challenge = challengeState,
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
    showChallengeDialog: Boolean = false,
    secondsRemaining: Int? = null,
    isReadyToStart: Boolean = false,
) =
    ChallengeState(
        challengeId = challengeId,
        challengerStates = challengers.values.map { challenger -> challenger.toChallengerState() },
        showChallengeDialog = showChallengeDialog,
        secondsRemaining = secondsRemaining,
        navigateToArena = isReadyToStart,
    )

internal fun Challenger.toChallengerState() =
    ChallengerState(
        userId = userId,
        displayName = displayName,
        isInstigator = isInstigator,
        status = status,
    )
