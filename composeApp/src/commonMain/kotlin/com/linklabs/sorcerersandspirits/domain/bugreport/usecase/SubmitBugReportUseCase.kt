package com.linklabs.sorcerersandspirits.domain.bugreport.usecase

import com.linklabs.sorcerersandspirits.domain.bugreport.BugReport
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportDataSource
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportResult
import com.linklabs.sorcerersandspirits.domain.platform.PlatformInfoProvider
import com.linklabs.sorcerersandspirits.domain.profile.usecase.GetProfileUseCase
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider

class SubmitBugReportUseCase(
    private val getProfileUseCase: GetProfileUseCase,
    private val currentAppVersionProvider: CurrentAppVersionProvider,
    private val platformInfoProvider: PlatformInfoProvider,
    private val bugReportDataSource: BugReportDataSource,
) {
    suspend operator fun invoke(message: String): BugReportResult {
        val profile = getProfileUseCase() ?: return BugReportResult.Failure(message = null)

        val report =
            BugReport(
                message = message,
                userId = profile.userId,
                displayName = profile.displayName,
                appVersion = currentAppVersionProvider.versionCode().toString(),
                platform = platformInfoProvider.platformName(),
            )

        return bugReportDataSource.submit(report = report)
    }
}
