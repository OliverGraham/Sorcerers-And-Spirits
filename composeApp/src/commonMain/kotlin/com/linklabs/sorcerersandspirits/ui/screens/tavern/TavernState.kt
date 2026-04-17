package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.utilities.SIXTY_SECONDS

@Immutable
data class TavernState(
    val userTraveler: TravelerState,
    val otherTravelers: List<TravelerState>,
    val challenge: ChallengeState?,
    val characterClasses: List<CharacterClass> =
        CharacterClass.getAllClasses(), // TODO: any reason to get from config?
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
    val secondsRemaining: Int,
    val showChallengeDialog: Boolean,
    val isThisUserTheInstigator: Boolean,
    val navigateToArena: Boolean = false, // TODO: Move this out of state?
) {
    val instigator: ChallengerState? =
        challengerStates.find { challengerState -> challengerState.isInstigator }

    private val minutes = secondsRemaining / SIXTY_SECONDS
    private val seconds = secondsRemaining % SIXTY_SECONDS

    val formattedTimeRemaining = "$minutes:${seconds.toString().padStart(2, '0')}"
    val isLessThanTenSeconds = secondsRemaining <= 10

    fun getExpectedNumberOfPlayers() =
        challengerStates.count { it.status == ChallengerStatus.Accepted }
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
    isUserInstigator: Boolean,
    secondsRemaining: Int = 0,
    showChallengeDialog: Boolean = false,
    isReadyToStart: Boolean = false,
) =
    ChallengeState(
        challengeId = challengeId,
        challengerStates = challengers.values.map { challenger -> challenger.toChallengerState() },
        isThisUserTheInstigator = isUserInstigator,
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
