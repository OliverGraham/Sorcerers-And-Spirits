package com.linklabs.sorcerersandspirits.domain.bugreport.usecase

import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportConfigProvider

class IsBugReportingEnabledUseCase(private val bugReportConfigProvider: BugReportConfigProvider) {
    operator fun invoke(): Boolean = bugReportConfigProvider.bugReportEndpointUrl().isNotBlank()
}
