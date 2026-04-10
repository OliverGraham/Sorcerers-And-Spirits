package com.linklabs.sorcerersandspirits.domain.tavern

// TODO: Look into time part
data class Challenge(
    val challengeId: String,
    val challengers: Map<String, Challenger>,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
) {
    fun isReadyToStart(minimumNumberOfChallengers: Int = MINIMUM_CHALLENGERS): Boolean {
        val activeChallengers =
            challengers.values.filterNot { it.status == ChallengerStatus.Declined }

        return activeChallengers.size >= minimumNumberOfChallengers &&
            activeChallengers.all { it.status == ChallengerStatus.Accepted }
    }

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
