package com.linklabs.sorcerersandspirits.domain.tavern

// TODO: Look into time part
data class Challenge(
    val challengeId: String,
    val challengers: Map<String, Challenger>,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
)

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
