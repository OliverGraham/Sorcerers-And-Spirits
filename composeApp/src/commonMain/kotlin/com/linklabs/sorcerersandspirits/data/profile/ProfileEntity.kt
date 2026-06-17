package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.domain.profile.Profile
import kotlinx.serialization.Serializable

@Serializable
data class ProfileEntity(
    val userId: String = "",
    val displayName: String = "",
    val stats: Map<String, Long> = emptyMap(),
)

internal fun ProfileEntity.toDomain() =
    Profile(userId = userId, displayName = displayName, stats = stats)

internal fun Profile.toEntity() =
    ProfileEntity(userId = userId, displayName = displayName, stats = stats)
