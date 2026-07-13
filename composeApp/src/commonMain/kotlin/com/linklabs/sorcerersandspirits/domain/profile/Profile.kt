package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId

/** @property [unlockedAchievements] mapped to their unlock time in epoch millis. */
data class Profile(
    val userId: String,
    val displayName: String,
    val stats: ProfileStats = ProfileStats(),
    val unlockedAchievements: Map<AchievementId, Long> = emptyMap(),
)
