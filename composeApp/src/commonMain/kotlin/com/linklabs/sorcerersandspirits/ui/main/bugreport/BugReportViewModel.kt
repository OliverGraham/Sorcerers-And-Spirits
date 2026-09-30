package com.linklabs.sorcerersandspirits.ui.main.bugreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportResult
import com.linklabs.sorcerersandspirits.domain.bugreport.usecase.IsBugReportingEnabledUseCase
import com.linklabs.sorcerersandspirits.domain.bugreport.usecase.SubmitBugReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class BugReportViewModel(
    private val submitBugReportUseCase: SubmitBugReportUseCase,
    private val isBugReportingEnabledUseCase: IsBugReportingEnabledUseCase,
) : ViewModel() {

    private val _isBugReportingEnabledFlow =
        MutableStateFlow(value = isBugReportingEnabledUseCase())
    val isBugReportingEnabledFlow = _isBugReportingEnabledFlow.asStateFlow()

    private val _submissionStateFlow =
        MutableStateFlow<BugReportSubmissionState>(value = BugReportSubmissionState.Idle)
    val submissionStateFlow = _submissionStateFlow.asStateFlow()

    fun refreshBugReportingEnabled() {
        _isBugReportingEnabledFlow.value = isBugReportingEnabledUseCase()
    }

    fun submit(message: String) {
        val isSending = _submissionStateFlow.value is BugReportSubmissionState.Sending
        if (isSending) return

        _submissionStateFlow.value = BugReportSubmissionState.Sending

        viewModelScope.launch {
            val result = submitBugReportUseCase(message = message)
            _submissionStateFlow.value = when (result) {
                is BugReportResult.Success -> BugReportSubmissionState.Sent
                is BugReportResult.Failure -> BugReportSubmissionState.Error
            }
        }
    }

    fun reset() {
        _submissionStateFlow.value = BugReportSubmissionState.Idle
    }
}
