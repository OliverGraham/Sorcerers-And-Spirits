package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_MILLIS
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeEntity(
    val challengeId: String = "",
    val challengerEntitiesMap: Map<String, ChallengerEntity> = emptyMap(),
    val startTimeMillis: Long? = null,
) {
    companion object {
        val challengerEntitiesMapName = ChallengeEntity::challengerEntitiesMap.name
    }
}

@Serializable
data class ChallengerEntity(
    val userId: String = "",
    val displayName: String = "",
    val isInstigator: Boolean = false,
    val status: ChallengerStatusEntity = ChallengerStatusEntity.Pending,
) {
    companion object {
        val statusName = ChallengerEntity::status.name
    }
}

@Serializable
enum class ChallengerStatusEntity {
    Accepted,
    Declined,
    Pending,
}

fun ChallengerStatus.toEntity(): ChallengerStatusEntity = when (this) {
    ChallengerStatus.Accepted -> ChallengerStatusEntity.Accepted
    ChallengerStatus.Declined -> ChallengerStatusEntity.Declined
    ChallengerStatus.Pending -> ChallengerStatusEntity.Pending
}

fun ChallengerStatusEntity.toDomain(): ChallengerStatus = when (this) {
    ChallengerStatusEntity.Accepted -> ChallengerStatus.Accepted
    ChallengerStatusEntity.Declined -> ChallengerStatus.Declined
    ChallengerStatusEntity.Pending -> ChallengerStatus.Pending
}

fun Challenger.toEntity(): ChallengerEntity = ChallengerEntity(
    userId = userId,
    displayName = displayName,
    isInstigator = isInstigator,
    status = status.toEntity(),
)

fun ChallengerEntity.toDomain(): Challenger = Challenger(
    userId = userId,
    displayName = displayName,
    isInstigator = isInstigator,
    status = status.toDomain(),
)

fun ChallengeEntity.toDomain(): Challenge = Challenge(
    challengeId = challengeId,
    challengers = challengerEntitiesMap.mapValues { (_, challenger) -> challenger.toDomain() },
    endTimeMillis = startTimeMillis?.plus(other = CHALLENGE_DURATION_MILLIS),
)
