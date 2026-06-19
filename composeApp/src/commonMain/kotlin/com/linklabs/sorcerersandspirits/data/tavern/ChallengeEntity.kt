package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_MILLIS
import kotlinx.serialization.Serializable

/**
 * Persisted challenge state. [startTimeMillis] is the single server-stamped epoch start (written as
 * RTDB `ServerValue.TIMESTAMP`), so every client shares one authoritative clock; the deadline the
 * clients count down to is derived as [startTimeMillis] + `CHALLENGE_DURATION_MILLIS` in
 * [toDomain]. Null until the server resolves the timestamp.
 */
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
        endTimeMillis = startTimeMillis?.plus(other = CHALLENGE_DURATION_MILLIS),
    )
