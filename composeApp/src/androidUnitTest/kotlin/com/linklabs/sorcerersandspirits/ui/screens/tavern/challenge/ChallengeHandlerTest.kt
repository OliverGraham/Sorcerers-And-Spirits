package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.SendChallengeUseCase
import com.linklabs.sorcerersandspirits.domain.tavern.usecase.UpdateChallengerStatusUseCase
import com.linklabs.sorcerersandspirits.domain.utilities.Timer
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeHandlerTest {

    private fun TestScope.handler() =
        ChallengeHandler(
            challengeTimer =
                Timer(coroutineScope = backgroundScope, onTimerTick = {}, onTimerExpiration = {}),
            sendChallengeUseCase = mockk<SendChallengeUseCase>(relaxed = true),
            updateChallengerStatusUseCase = mockk<UpdateChallengerStatusUseCase>(relaxed = true),
            now = { NOW },
        )

    @Test
    fun `a freshly observed challenge resumes from the communal deadline, not a fresh sixty seconds`() =
        runTest {
            // Challenge created 35s ago with a 60s deadline -> 25s left, as on an app restart.
            val challenge = challenge(endTimeMillis = NOW + 25_000L)

            val update =
                handler()
                    .handleChallengeUpdate(
                        previousChallengeState = null,
                        updatedChallenge = challenge,
                        userId = USER_ID,
                    )

            val challengeState = (update as ChallengeUpdate.Ongoing).challengeState
            assertEquals(expected = 25, actual = challengeState?.secondsRemaining)
        }

    private fun challenge(endTimeMillis: Long): Challenge =
        Challenge(
            challengeId = "challenge-1",
            challengers =
                mapOf(
                    USER_ID to
                        Challenger(
                            userId = USER_ID,
                            displayName = "Invitee",
                            isInstigator = false,
                            status = ChallengerStatus.Pending,
                        )
                ),
            startTimeMillis = endTimeMillis - 60_000L,
            endTimeMillis = endTimeMillis,
        )

    private companion object {
        const val NOW = 1_000_000L
        const val USER_ID = "user-1"
    }
}
