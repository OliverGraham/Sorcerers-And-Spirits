package com.linklabs.sorcerersandspirits.domain.bugreport

sealed interface BugReportResult {
    data object Success : BugReportResult

    data class Failure(val message: String?) : BugReportResult
}
