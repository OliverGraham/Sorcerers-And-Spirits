package com.linklabs.sorcerersandspirits.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun createProfile(profile: Profile): DisplayNameResult

    suspend fun deleteProfile(userId: String)

    suspend fun getProfile(userId: String): Profile?

    /** The [limit] profiles with the most Gauntlets won, highest first. */
    suspend fun getTopProfiles(limit: Int): List<Profile>

    fun getProfileFlow(userId: String): Flow<Profile?>

    suspend fun updateDisplayName(userId: String, newDisplayName: String): DisplayNameResult

    suspend fun incrementStats(userId: String, delta: ProfileStats)
}
