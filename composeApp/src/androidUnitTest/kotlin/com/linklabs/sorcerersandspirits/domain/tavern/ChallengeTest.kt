package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_SECONDS
import kotlin.test.Test
import kotlin.test.assertEquals

class ChallengeTest {

    @Test
    fun `secondsRemaining counts down to the communal deadline`() {
        val challenge = challenge(endTimeMillis = NOW + 40_000L)

        assertEquals(expected = 40, actual = challenge.secondsRemaining(nowMillis = NOW))
    }

    @Test
    fun `secondsRemaining rounds partial seconds down`() {
        val challenge = challenge(endTimeMillis = NOW + 40_999L)

        assertEquals(expected = 40, actual = challenge.secondsRemaining(nowMillis = NOW))
    }

    @Test
    fun `secondsRemaining floors at zero once the deadline has passed`() {
        val challenge = challenge(endTimeMillis = NOW - 5_000L)

        assertEquals(expected = 0, actual = challenge.secondsRemaining(nowMillis = NOW))
    }

    @Test
    fun `secondsRemaining falls back to the full duration when no deadline is set`() {
        val challenge = challenge(endTimeMillis = null)

        assertEquals(
            expected = CHALLENGE_DURATION_SECONDS,
            actual = challenge.secondsRemaining(nowMillis = NOW),
        )
    }

    private fun challenge(endTimeMillis: Long?): Challenge =
        Challenge(
            challengeId = "challenge-1",
            challengers = emptyMap(),
            endTimeMillis = endTimeMillis,
        )

    private companion object {
        const val NOW = 1_000_000L
    }
}
