package com.linklabs.sorcerersandspirits.domain.profile

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun createProfile(profile: Profile): DisplayNameResult

    suspend fun deleteProfile(userId: String)

    suspend fun getProfile(userId: String): Profile?

    suspend fun getTopProfiles(limit: Int): List<Profile>

    fun getProfileFlow(userId: String): Flow<Profile?>

    suspend fun updateDisplayName(userId: String, newDisplayName: String): DisplayNameResult

    suspend fun incrementStats(userId: String, delta: ProfileStats)

    /**
     * Records each of [achievementIds] as unlocked at [unlockedAt] (epoch millis), leaving any
     * already-recorded unlocks untouched.
     */
    suspend fun unlockAchievements(
        userId: String,
        achievementIds: List<AchievementId>,
        unlockedAt: Long,
    )
}
