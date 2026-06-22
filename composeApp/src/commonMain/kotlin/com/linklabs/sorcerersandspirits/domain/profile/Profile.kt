package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId

data class Profile(
    val userId: String,
    val displayName: String,
    val stats: ProfileStats = ProfileStats(),
    /** Unlocked achievements mapped to their unlock time in epoch millis. */
    val unlockedAchievements: Map<AchievementId, Long> = emptyMap(),
)
