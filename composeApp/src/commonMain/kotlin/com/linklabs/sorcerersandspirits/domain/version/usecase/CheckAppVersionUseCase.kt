package com.linklabs.sorcerersandspirits.domain.version.usecase

import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.version.SupportedAppVersionProvider

/**
 * Compares the running build against the published version thresholds. Reads the currently active
 * RemoteConfig values, so call it only after the config has been fetched/activated at app start.
 */
class CheckAppVersionUseCase(
    private val currentAppVersionProvider: CurrentAppVersionProvider,
    private val supportedAppVersionProvider: SupportedAppVersionProvider,
) {
    operator fun invoke(): AppUpgradeStatus {
        val currentVersion = currentAppVersionProvider.versionCode()
        val isBelowMinimum =
            currentVersion < supportedAppVersionProvider.minimumSupportedVersionCode()
        val isBehindLatest = currentVersion < supportedAppVersionProvider.latestVersionCode()

        return when {
            isBelowMinimum -> AppUpgradeStatus.FORCE_UPGRADE
            isBehindLatest -> AppUpgradeStatus.UPDATE_AVAILABLE
            else -> AppUpgradeStatus.UP_TO_DATE
        }
    }
}
