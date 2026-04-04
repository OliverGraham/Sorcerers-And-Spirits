package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

@Immutable
data class TavernState(
    val userId: String,
    val travelers: List<TravelerState>,
    val challenge: ChallengeState? = null,
) {
    // TODO: Write tests for this
    val userTraveler = travelers.first { traveler -> traveler.userId == userId }
    val otherTravelers = travelers.filterNot { traveler -> traveler == userTraveler }

    val userChallenger =
        challenge?.challengerStates?.firstOrNull { challenger -> challenger.userId == userId }
}

@Immutable
data class TravelerState(
    val userId: String,
    val displayName: String,
    val tavernStatus: TavernStatus,
)

@Immutable
data class ChallengerState(
    val userId: String,
    val displayName: String,
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

internal fun List<Traveler>.toTavernState(
    userId: String,
    updatedChallenge: Challenge?,
    challengeBeforeUpdate: ChallengeState?,
): TavernState {
    // if oldChallenge is showing and updatedChallenge isn't null, keep showing
    // if oldChallenge is null and updatedChallenge isn't null, keep showing

    val showChallengeDialog =
        when {
            challengeBeforeUpdate == null && updatedChallenge != null -> true
            challengeBeforeUpdate != null &&
                challengeBeforeUpdate.showChallengeDialog &&
                updatedChallenge != null -> true
            else -> false
        }
    challengeBeforeUpdate?.showChallengeDialog == true && updatedChallenge != null
    val secondsRemaining =
        challengeBeforeUpdate?.secondsRemaining ?: ChallengeHandler.COUNTDOWN_SECONDS

    return TavernState(
        userId = userId,
        travelers = map { traveler -> traveler.toTravelerState() },
        challenge =
            updatedChallenge?.toChallengeState(
                showChallengeDialog = showChallengeDialog,
                secondsRemaining = secondsRemaining,
            ),
    )
}

private fun Traveler.toTravelerState(): TravelerState =
    TravelerState(userId = userId, displayName = displayName, tavernStatus = tavernStatus)

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

internal fun Challenger.toChallengerState() =
    ChallengerState(
        userId = userId,
        displayName = displayName,
        isInstigator = isInstigator,
        status = status,
    )
