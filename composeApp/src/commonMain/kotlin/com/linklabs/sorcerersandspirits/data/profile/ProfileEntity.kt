package com.linklabs.sorcerersandspirits.data.profile

import com.linklabs.sorcerersandspirits.data.FireStoreKeys
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlinx.serialization.Serializable

@Serializable
data class ProfileEntity(
    val userId: String = "",
    val displayName: String = "",
    val stats: ProfileStatsEntity = ProfileStatsEntity(),
    val unlockedAchievements: Map<String, Long> = emptyMap(),
)

@Serializable data class DisplayNameEntity(val userId: String = "", val displayName: String = "")

@Serializable
data class ProfileStatsEntity(
    val overall: GameStatsEntity = GameStatsEntity(),
    val perClass: Map<String, GameStatsEntity> = emptyMap(),
)

@Serializable
data class GameStatsEntity(
    val matchesPlayed: Long = 0,
    val matchesWon: Long = 0,
    val matchesLost: Long = 0,
    val matchesForfeited: Long = 0,
    val gauntletsPlayed: Long = 0,
    val gauntletsWon: Long = 0,
    val gauntletsLost: Long = 0,
    val gauntletsForfeited: Long = 0,
    val knockoutsDealt: Long = 0,
    val attacksMade: Long = 0,
    val abilitiesUsed: Long = 0,
    val timesSelected: Long = 0,
)

internal fun ProfileEntity.toDomain() = Profile(
    userId = userId,
    displayName = displayName,
    stats = stats.toDomain(),
    unlockedAchievements =
    unlockedAchievements
        .mapNotNull { (idName, unlockedAt) ->
            val achievementId =
                AchievementId.entries.find { it.name == idName } ?: return@mapNotNull null
            achievementId to unlockedAt
        }
        .toMap(),
)

internal fun Profile.toEntity() = ProfileEntity(
    userId = userId,
    displayName = displayName,
    stats = stats.toEntity(),
    unlockedAchievements =
    unlockedAchievements.entries.associate { (achievementId, unlockedAt) ->
        achievementId.name to unlockedAt
    },
)

internal fun ProfileStatsEntity.toDomain() = ProfileStats(
    overall = overall.toDomain(),
    perClass =
    perClass
        .mapNotNull { (className, entity) ->
            val classType = CharacterClassType.entries.find { it.name == className }
            if (classType == null) null else classType to entity.toDomain()
        }
        .toMap(),
)

internal fun ProfileStats.toEntity() = ProfileStatsEntity(
    overall = overall.toEntity(),
    perClass =
    perClass.entries.associate { (classType, stats) -> classType.name to stats.toEntity() },
)

internal fun GameStatsEntity.toDomain() = GameStats(
    matchesPlayed = matchesPlayed,
    matchesWon = matchesWon,
    matchesLost = matchesLost,
    matchesForfeited = matchesForfeited,
    gauntletsPlayed = gauntletsPlayed,
    gauntletsWon = gauntletsWon,
    gauntletsLost = gauntletsLost,
    gauntletsForfeited = gauntletsForfeited,
    knockoutsDealt = knockoutsDealt,
    attacksMade = attacksMade,
    abilitiesUsed = abilitiesUsed,
    timesSelected = timesSelected,
)

internal fun GameStats.toEntity() = GameStatsEntity(
    matchesPlayed = matchesPlayed,
    matchesWon = matchesWon,
    matchesLost = matchesLost,
    matchesForfeited = matchesForfeited,
    gauntletsPlayed = gauntletsPlayed,
    gauntletsWon = gauntletsWon,
    gauntletsLost = gauntletsLost,
    gauntletsForfeited = gauntletsForfeited,
    knockoutsDealt = knockoutsDealt,
    attacksMade = attacksMade,
    abilitiesUsed = abilitiesUsed,
    timesSelected = timesSelected,
)

internal fun ProfileStats.toStatIncrements(): Map<List<String>, Int> = buildMap {
    overall.toFieldIncrements().forEach { (statKey, amount) ->
        put(listOf(FireStoreKeys.STATS, FireStoreKeys.OVERALL, statKey), amount)
    }
    perClass.forEach { (classType, stats) ->
        stats.toFieldIncrements().forEach { (statKey, amount) ->
            put(
                listOf(FireStoreKeys.STATS, FireStoreKeys.PER_CLASS, classType.name, statKey),
                amount,
            )
        }
    }
}

private fun GameStats.toFieldIncrements(): Map<String, Int> = buildMap {
    if (matchesPlayed != 0L) put(FireStoreKeys.MATCHES_PLAYED, matchesPlayed.toInt())
    if (matchesWon != 0L) put(FireStoreKeys.MATCHES_WON, matchesWon.toInt())
    if (matchesLost != 0L) put(FireStoreKeys.MATCHES_LOST, matchesLost.toInt())
    if (matchesForfeited != 0L) put(FireStoreKeys.MATCHES_FORFEITED, matchesForfeited.toInt())
    if (gauntletsPlayed != 0L) put(FireStoreKeys.GAUNTLETS_PLAYED, gauntletsPlayed.toInt())
    if (gauntletsWon != 0L) put(FireStoreKeys.GAUNTLETS_WON, gauntletsWon.toInt())
    if (gauntletsLost != 0L) put(FireStoreKeys.GAUNTLETS_LOST, gauntletsLost.toInt())
    if (gauntletsForfeited != 0L) {
        put(FireStoreKeys.GAUNTLETS_FORFEITED, gauntletsForfeited.toInt())
    }
    if (knockoutsDealt != 0L) put(FireStoreKeys.KNOCKOUTS_DEALT, knockoutsDealt.toInt())
    if (attacksMade != 0L) put(FireStoreKeys.ATTACKS_MADE, attacksMade.toInt())
    if (abilitiesUsed != 0L) put(FireStoreKeys.ABILITIES_USED, abilitiesUsed.toInt())
    if (timesSelected != 0L) put(FireStoreKeys.TIMES_SELECTED, timesSelected.toInt())
}
