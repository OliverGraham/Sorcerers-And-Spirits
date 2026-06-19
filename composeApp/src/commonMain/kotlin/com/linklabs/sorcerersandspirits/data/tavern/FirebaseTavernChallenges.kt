package com.linklabs.sorcerersandspirits.data.tavern

import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGES_PATH
import com.linklabs.sorcerersandspirits.data.FireStoreKeys.CHALLENGE_MAILBOXES_PATH
import com.linklabs.sorcerersandspirits.domain.utilities.CHALLENGE_DURATION_MILLIS
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun MutableMap<String, Any?>.removeChallenge(challengeId: String) {
    this[challengeId.toChallengesPath()] = null
}

internal fun MutableMap<String, Any?>.clearMailbox(userId: String) {
    this[userId.toChallengeMailBoxesPath()] = null
}

internal fun MutableMap<String, Any?>.clearMailboxes(userIds: List<String>) {
    userIds.forEach(action = ::clearMailbox)
}

@OptIn(ExperimentalTime::class)
internal fun ChallengeEntity.isExpired(): Boolean {
    val startTime = startTimeMillis ?: return false
    return startTime + CHALLENGE_DURATION_MILLIS <= Clock.System.now().toEpochMilliseconds()
}

internal fun String.toChallengesPath(): String = "$CHALLENGES_PATH/$this"

internal fun String.toChallengeMailBoxesPath(): String = "$CHALLENGE_MAILBOXES_PATH/$this"
