package com.linklabs.sorcerersandspirits.domain.tavern

data class Challenge(val challengeId: String, val challengers: Map<String, Challenger>)

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
