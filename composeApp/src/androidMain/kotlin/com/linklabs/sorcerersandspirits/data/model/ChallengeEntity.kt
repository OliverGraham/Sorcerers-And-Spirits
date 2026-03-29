package com.linklabs.sorcerersandspirits.data.model

import com.google.firebase.database.PropertyName
import com.linklabs.sorcerersandspirits.domain.tavern.Challenge
import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeEntity(
    val challengeId: String = "",
    val challengers: Map<String, ChallengerEntity> = emptyMap(),
)

@Serializable
data class ChallengerEntity(
    val userId: String = "",
    val displayName: String = "",
    @get:PropertyName("isInstigator") val isInstigator: Boolean = false,
    val status: ChallengerStatus = ChallengerStatus.Pending,
)

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
        challengers = challengers.mapValues { (_, challenger) -> challenger.toDomain() },
    )
