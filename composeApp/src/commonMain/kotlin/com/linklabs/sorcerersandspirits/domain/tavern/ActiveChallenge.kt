package com.linklabs.sorcerersandspirits.domain.tavern

/**
 * The active challenge for a specific user, paired with that user's id so the challenge can be
 * interpreted (instigator vs. invitee, ready-to-start) without a separate userId lookup.
 */
data class ActiveChallenge(val userId: String, val challenge: Challenge?)
