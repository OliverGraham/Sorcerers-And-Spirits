package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementCatalog
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Re-evaluates every achievement against the signed-in user's freshly-written stats and records any
 * that are newly satisfied. Idempotent: already-unlocked achievements are left untouched, so
 * running it after each stats write (and on launch replay) only ever adds.
 */
@OptIn(ExperimentalTime::class)
class UnlockAchievementsUseCase(
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(userId: String) {
        val profile = profileRepository.getProfile(userId = userId) ?: return

        val lockedAchievements =
            AchievementCatalog.all.filterNot { achievement ->
                profile.unlockedAchievements.containsKey(achievement.id)
            }
        val newlyUnlocked =
            lockedAchievements
                .filter { achievement -> achievement.isUnlockedBy(stats = profile.stats) }
                .map { achievement -> achievement.id }
        if (newlyUnlocked.isEmpty()) return

        profileRepository.unlockAchievements(
            userId = userId,
            achievementIds = newlyUnlocked,
            unlockedAt = clock.now().toEpochMilliseconds(),
        )
    }
}
