package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.domain.tavern.Challenger
import com.linklabs.sorcerersandspirits.domain.tavern.ChallengerStatus
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus

/** Shared builders for Tavern test data, keeping the test bodies focused on the scenario. */
fun travelerEntity(
    userId: String,
    displayName: String = userId,
    tavernStatus: TavernStatus = TavernStatus.Available,
): TravelerEntity =
    TravelerEntity(userId = userId, displayName = displayName, tavernStatus = tavernStatus)

fun challengerEntity(
    userId: String,
    displayName: String = userId,
    isInstigator: Boolean = false,
    status: ChallengerStatus = ChallengerStatus.Pending,
): ChallengerEntity = ChallengerEntity(
    userId = userId,
    displayName = displayName,
    isInstigator = isInstigator,
    status = status,
)

fun challengeEntity(
    challengeId: String,
    challengers: List<ChallengerEntity>,
    startTimeMillis: Long? = 0L,
): ChallengeEntity = ChallengeEntity(
    challengeId = challengeId,
    challengerEntitiesMap = challengers.associateBy { it.userId },
    startTimeMillis = startTimeMillis,
)

fun challenger(
    userId: String,
    displayName: String = userId,
    isInstigator: Boolean = false,
    status: ChallengerStatus = ChallengerStatus.Pending,
): Challenger = Challenger(
    userId = userId,
    displayName = displayName,
    isInstigator = isInstigator,
    status = status,
)
