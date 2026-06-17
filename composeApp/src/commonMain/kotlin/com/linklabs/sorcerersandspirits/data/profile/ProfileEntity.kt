package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlinx.serialization.Serializable

@Serializable
data class ProfileEntity(
    val userId: String = "",
    val displayName: String = "",
    val stats: ProfileStatsEntity = ProfileStatsEntity(),
)

@Serializable data class ProfileStatsEntity(val wins: Long = 0, val losses: Long = 0)

internal fun ProfileEntity.toDomain() =
    Profile(userId = userId, displayName = displayName, stats = stats.toDomain())

internal fun Profile.toEntity() =
    ProfileEntity(userId = userId, displayName = displayName, stats = stats.toEntity())

internal fun ProfileStatsEntity.toDomain() = ProfileStats(wins = wins, losses = losses)

internal fun ProfileStats.toEntity() = ProfileStatsEntity(wins = wins, losses = losses)

internal fun ProfileStats.toStatIncrements(): Map<String, Int> = buildMap {
    if (wins != 0L) put(FireStoreKeys.WINS, wins.toInt())
    if (losses != 0L) put(FireStoreKeys.LOSSES, losses.toInt())
}
