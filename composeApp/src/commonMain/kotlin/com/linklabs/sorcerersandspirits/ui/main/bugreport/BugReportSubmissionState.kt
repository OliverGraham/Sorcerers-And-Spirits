package com.linklabs.sorcerersandspirits.ui.main.bugreport

sealed interface BugReportSubmissionState {
    data object Idle : BugReportSubmissionState

    data object Sending : BugReportSubmissionState

    data object Sent : BugReportSubmissionState

    data object Error : BugReportSubmissionState
}
