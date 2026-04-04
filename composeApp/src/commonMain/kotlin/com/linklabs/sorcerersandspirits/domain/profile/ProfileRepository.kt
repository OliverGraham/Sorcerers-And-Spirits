package com.linklabs.sorcerersandspirits.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun createProfile(profile: Profile)

    suspend fun deleteProfile(userId: String)

    suspend fun getProfile(userId: String): Profile?

    fun getProfileFlow(userId: String): Flow<Profile?>

    suspend fun updateDisplayName(userId: String, newDisplayName: String)
}
