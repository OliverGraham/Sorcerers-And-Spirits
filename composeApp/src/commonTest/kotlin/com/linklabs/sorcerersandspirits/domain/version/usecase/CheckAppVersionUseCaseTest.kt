package com.linklabs.sorcerersandspirits.domain.version.usecase

import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import com.linklabs.sorcerersandspirits.domain.version.SupportedAppVersionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckAppVersionUseCaseTest {

    @Test
    fun `Given the build is below the minimum supported version, then it force-upgrades`() {
        val status =
            checkVersion(currentVersion = 3, minimumSupportedVersion = 5, latestVersion = 9)

        assertEquals(expected = AppUpgradeStatus.FORCE_UPGRADE, actual = status)
    }

    @Test
    fun `Given the build meets the minimum but trails the latest, then it nudges an update`() {
        val status =
            checkVersion(currentVersion = 5, minimumSupportedVersion = 5, latestVersion = 9)

        assertEquals(expected = AppUpgradeStatus.UPDATE_AVAILABLE, actual = status)
    }

    @Test
    fun `Given the build is at the latest version, then it is up to date`() {
        val status =
            checkVersion(currentVersion = 9, minimumSupportedVersion = 5, latestVersion = 9)

        assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = status)
    }

    @Test
    fun `Given the build is ahead of the latest version, then it is up to date`() {
        val status =
            checkVersion(currentVersion = 12, minimumSupportedVersion = 5, latestVersion = 9)

        assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = status)
    }

    @Test
    fun `Given an unconfigured backend with zeroed thresholds, then any real build is up to date`() {
        val status =
            checkVersion(currentVersion = 1, minimumSupportedVersion = 0, latestVersion = 0)

        assertEquals(expected = AppUpgradeStatus.UP_TO_DATE, actual = status)
    }

    private fun checkVersion(
        currentVersion: Long,
        minimumSupportedVersion: Long,
        latestVersion: Long,
    ): AppUpgradeStatus =
        CheckAppVersionUseCase(
                currentAppVersionProvider =
                    object : CurrentAppVersionProvider {
                        override fun versionCode() = currentVersion
                    },
                supportedAppVersionProvider =
                    object : SupportedAppVersionProvider {
                        override fun minimumSupportedVersionCode() = minimumSupportedVersion

                        override fun latestVersionCode() = latestVersion
                    },
            )
            .invoke()
}
