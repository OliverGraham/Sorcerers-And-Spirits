package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_SECONDS
import com.linklabs.sorcerersandspirits.domain.utilities.ONE_SECOND

data class Challenge(
    val challengeId: String,
    val challengers: Map<String, Challenger>,
    val endTimeMillis: Long? = null,
) {
    fun secondsRemaining(nowMillis: Long): Int {
        val deadlineMillis = endTimeMillis ?: return CHALLENGE_DURATION_SECONDS
        return ((deadlineMillis - nowMillis) / ONE_SECOND).toInt().coerceAtLeast(minimumValue = 0)
    }

    fun isReadyToStart(
        userId: String,
        minimumNumberOfChallengers: Int = MINIMUM_CHALLENGERS,
    ): Boolean {
        val hasUserChallengerAccepted = challengers[userId]?.status == ChallengerStatus.Accepted
        if (!hasUserChallengerAccepted) return false

        val activeChallengers =
            challengers.values.filterNot { it.status == ChallengerStatus.Declined }

        return activeChallengers.size >= minimumNumberOfChallengers &&
            activeChallengers.all { it.status == ChallengerStatus.Accepted }
    }

    fun isUserInstigator(userId: String) = challengers[userId]?.isInstigator == true

    companion object {
        const val MINIMUM_CHALLENGERS = 2
    }
}

data class Challenger(
    val userId: String,
    val displayName: String,
    val isInstigator: Boolean,
    val status: ChallengerStatus,
)

enum class ChallengerStatus {
    Accepted,
    Declined,
    Pending,
}
