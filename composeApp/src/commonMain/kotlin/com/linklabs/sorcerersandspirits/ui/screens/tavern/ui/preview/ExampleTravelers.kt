package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview

import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengerState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState

/** Reusable sample state for Tavern composable previews. */
val exampleUserTraveler =
    TravelerState(
        userId = "user_me",
        displayName = "You",
        isThisTravelerTheUser = true,
        tavernStatus = TavernStatus.Available,
    )

val exampleOtherTravelers =
    listOf(
        TravelerState(
            userId = "user_1",
            displayName = "Merlin",
            isThisTravelerTheUser = false,
            tavernStatus = TavernStatus.Available,
        ),
        TravelerState(
            userId = "user_2",
            displayName = "Morgana",
            isThisTravelerTheUser = false,
            tavernStatus = TavernStatus.Available,
        ),
        TravelerState(
            userId = "user_3",
            displayName = "Lancelot",
            isThisTravelerTheUser = false,
            tavernStatus = TavernStatus.Busy,
        ),
        TravelerState(
            userId = "user_4",
            displayName = "Guinevere",
            isThisTravelerTheUser = false,
            tavernStatus = TavernStatus.Available,
        ),
    )

val exampleInstigatorChallengeState =
    ChallengeState(
        challengeId = "preview_challenge",
        challengerStates =
            listOf(
                ChallengerState(
                    userId = exampleUserTraveler.userId,
                    displayName = exampleUserTraveler.displayName,
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                ),
                ChallengerState(
                    userId = "user_1",
                    displayName = "Merlin",
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                ),
                ChallengerState(
                    userId = "user_2",
                    displayName = "Morgana",
                    isInstigator = false,
                    status = ChallengerStatus.Accepted,
                ),
            ),
        secondsRemaining = 45,
        showChallengeDialog = true,
        isThisUserTheInstigator = true,
    )

val exampleRecipientChallengeState =
    ChallengeState(
        challengeId = "preview_challenge",
        challengerStates =
            listOf(
                ChallengerState(
                    userId = "user_1",
                    displayName = "Merlin",
                    isInstigator = true,
                    status = ChallengerStatus.Accepted,
                ),
                ChallengerState(
                    userId = exampleUserTraveler.userId,
                    displayName = exampleUserTraveler.displayName,
                    isInstigator = false,
                    status = ChallengerStatus.Pending,
                ),
            ),
        secondsRemaining = 9,
        showChallengeDialog = true,
        isThisUserTheInstigator = false,
    )

val exampleTavernState =
    TavernState(
        userTraveler = exampleUserTraveler,
        otherTravelers = exampleOtherTravelers,
        challenge = null,
    )
