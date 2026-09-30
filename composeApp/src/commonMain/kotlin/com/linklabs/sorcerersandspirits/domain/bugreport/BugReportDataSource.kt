package com.linklabs.sorcerersandspirits.domain.bugreport

interface BugReportDataSource {
    suspend fun submit(report: BugReport): BugReportResult
}
