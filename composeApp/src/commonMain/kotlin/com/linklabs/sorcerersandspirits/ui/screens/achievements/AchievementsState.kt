package com.linklabs.sorcerersandspirits.ui.screens.achievements

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementCatalog
import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import com.linklabs.sorcerersandspirits.domain.profile.Profile

@Immutable
data class AchievementsState(
    val achievements: List<AchievementRowState>,
    val unlockedCount: Int,
    val totalCount: Int,
)

@Immutable data class AchievementRowState(val id: AchievementId, val isUnlocked: Boolean)

internal fun Profile.toAchievementsState(): AchievementsState {
    val unlockedIds = unlockedAchievements.keys

    val rows =
        AchievementCatalog.all.map { achievement ->
            AchievementRowState(id = achievement.id, isUnlocked = achievement.id in unlockedIds)
        }

    return AchievementsState(
        achievements = rows,
        unlockedCount = rows.count { row -> row.isUnlocked },
        totalCount = rows.size,
    )
}
