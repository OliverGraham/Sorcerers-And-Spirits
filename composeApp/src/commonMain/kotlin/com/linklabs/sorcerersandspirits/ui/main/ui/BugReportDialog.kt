package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.linklabs.sorcerersandspirits.ui.common.AppButton
import com.linklabs.sorcerersandspirits.ui.main.bugreport.BugReportSubmissionState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.bug_report_cancel
import sorcerersandspirits.composeapp.generated.resources.bug_report_close
import sorcerersandspirits.composeapp.generated.resources.bug_report_description
import sorcerersandspirits.composeapp.generated.resources.bug_report_error
import sorcerersandspirits.composeapp.generated.resources.bug_report_message_label
import sorcerersandspirits.composeapp.generated.resources.bug_report_sending
import sorcerersandspirits.composeapp.generated.resources.bug_report_sent
import sorcerersandspirits.composeapp.generated.resources.bug_report_submit
import sorcerersandspirits.composeapp.generated.resources.bug_report_title

@Composable
internal fun BugReportDialog(
    submissionState: BugReportSubmissionState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf(value = "") }

    val isSending = submissionState is BugReportSubmissionState.Sending
    val isSent = submissionState is BugReportSubmissionState.Sent

    Dialog(onDismissRequest = { if (!isSending) onDismiss() }) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(size = 24.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                Text(
                    text = stringResource(resource = Res.string.bug_report_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (isSent) {
                    Text(
                        text = stringResource(resource = Res.string.bug_report_sent),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    AppButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(resource = Res.string.bug_report_close))
                    }
                } else {
                    BugReportForm(
                        message = message,
                        onMessageChange = { message = it },
                        submissionState = submissionState,
                        isSending = isSending,
                        onSubmit = { onSubmit(message) },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun BugReportForm(
    message: String,
    onMessageChange: (String) -> Unit,
    submissionState: BugReportSubmissionState,
    isSending: Boolean,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text(
        text = stringResource(resource = Res.string.bug_report_description),
        style = MaterialTheme.typography.bodyMedium,
    )

    OutlinedTextField(
        value = message,
        onValueChange = onMessageChange,
        label = { Text(text = stringResource(resource = Res.string.bug_report_message_label)) },
        enabled = !isSending,
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )

    if (submissionState is BugReportSubmissionState.Error) {
        Text(
            text = stringResource(resource = Res.string.bug_report_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    val canSubmit = message.isNotBlank() && !isSending

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        AppButton(
            onClick = onDismiss,
            modifier = Modifier.weight(weight = 1f),
            isEnabled = !isSending,
            borderColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(text = stringResource(resource = Res.string.bug_report_cancel))
        }

        AppButton(
            onClick = onSubmit,
            modifier = Modifier.weight(weight = 1f),
            isEnabled = canSubmit,
        ) {
            val submitLabel =
                if (isSending) {
                    Res.string.bug_report_sending
                } else {
                    Res.string.bug_report_submit
                }
            Text(text = stringResource(resource = submitLabel))
        }
    }
}

@Preview
@Composable
private fun BugReportDialogPreview() {
    BugReportDialog(
        submissionState = BugReportSubmissionState.Idle,
        onSubmit = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun BugReportDialogErrorPreview() {
    BugReportDialog(
        submissionState = BugReportSubmissionState.Error,
        onSubmit = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun BugReportDialogSentPreview() {
    BugReportDialog(
        submissionState = BugReportSubmissionState.Sent,
        onSubmit = {},
        onDismiss = {},
    )
}
