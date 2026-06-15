package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_MILLIS
import kotlinx.serialization.Serializable

// TODO(SS-042): Look into time part
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
    val status: ChallengerStatus = ChallengerStatus.Pending,
) {
    companion object {
        val statusName = ChallengerEntity::status.name
    }
}

fun Challenger.toEntity(): ChallengerEntity =
    ChallengerEntity(
        userId = userId,
        displayName = displayName,
        isInstigator = isInstigator,
        status = status,
    )

fun ChallengerEntity.toDomain(): Challenger =
    Challenger(
        userId = userId,
        displayName = displayName,
        isInstigator = isInstigator,
        status = status,
    )

fun ChallengeEntity.toDomain(): Challenge =
    Challenge(
        challengeId = challengeId,
        challengers = challengerEntitiesMap.mapValues { (_, challenger) -> challenger.toDomain() },
        startTimeMillis = startTimeMillis,
        endTimeMillis = startTimeMillis?.plus(other = CHALLENGE_DURATION_MILLIS),
    )
