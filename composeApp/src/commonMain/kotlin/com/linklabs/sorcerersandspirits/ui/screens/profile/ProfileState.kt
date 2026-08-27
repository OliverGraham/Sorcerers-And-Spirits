package com.linklabs.sorcerersandspirits.ui.screens.profile

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementCatalog
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.Profile

@Immutable
data class ProfileState(
    val displayName: String,
    val rating: Long,
    val overall: GameStats,
    val perClass: List<ClassStatsState>,
    val achievementsUnlocked: Int,
    val achievementsTotal: Int,
) {
    val hasRecord: Boolean
        get() = !overall.isEmpty
}

@Immutable data class ClassStatsState(val classType: CharacterClassType, val stats: GameStats)

internal fun Profile.toProfileState(): ProfileState {
    val catalogIds = AchievementCatalog.all.map { achievement -> achievement.id }.toSet()

    return ProfileState(
        displayName = displayName,
        rating = stats.rating,
        overall = stats.overall,
        perClass =
        CharacterClass.playableTypes().mapNotNull { classType ->
            val classStats = stats.perClass[classType] ?: return@mapNotNull null
            if (classStats.isEmpty) {
                null
            } else {
                ClassStatsState(classType = classType, stats = classStats)
            }
        },
        achievementsUnlocked = unlockedAchievements.keys.count { id -> id in catalogIds },
        achievementsTotal = catalogIds.size,
    )
}
