package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.linklabs.sorcerersandspirits.domain.version.AppUpgradeStatus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.app_update_available_message
import sorcerersandspirits.composeapp.generated.resources.app_update_available_title
import sorcerersandspirits.composeapp.generated.resources.app_update_button
import sorcerersandspirits.composeapp.generated.resources.app_update_dismiss_button
import sorcerersandspirits.composeapp.generated.resources.app_update_force_message
import sorcerersandspirits.composeapp.generated.resources.app_update_force_title

/**
 * Gates the app on build version. A [AppUpgradeStatus.FORCE_UPGRADE] block is non-dismissible; an
 * [AppUpgradeStatus.UPDATE_AVAILABLE] nudge can be dismissed via [onDismiss].
 */
@Composable
fun AppUpgradeDialog(
    status: AppUpgradeStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val isForcedUpgrade = status == AppUpgradeStatus.FORCE_UPGRADE

    val titleResource =
        if (isForcedUpgrade) {
            Res.string.app_update_force_title
        } else {
            Res.string.app_update_available_title
        }
    val messageResource =
        if (isForcedUpgrade) {
            Res.string.app_update_force_message
        } else {
            Res.string.app_update_available_message
        }

    Dialog(
        onDismissRequest = { if (!isForcedUpgrade) onDismiss() },
        properties =
            DialogProperties(
                dismissOnBackPress = !isForcedUpgrade,
                dismissOnClickOutside = !isForcedUpgrade,
            ),
    ) {
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
                    text = stringResource(resource = titleResource),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = stringResource(resource = messageResource),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    if (!isForcedUpgrade) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(weight = 1f),
                        ) {
                            Text(
                                text =
                                    stringResource(resource = Res.string.app_update_dismiss_button)
                            )
                        }
                    }

                    Button(
                        onClick = { uriHandler.openUri(uri = appStoreUrl()) },
                        modifier = Modifier.weight(weight = 1f),
                    ) {
                        Text(text = stringResource(resource = Res.string.app_update_button))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppUpgradeDialogForcePreview() {
    AppUpgradeDialog(status = AppUpgradeStatus.FORCE_UPGRADE, onDismiss = {})
}

@Preview
@Composable
private fun AppUpgradeDialogAvailablePreview() {
    AppUpgradeDialog(status = AppUpgradeStatus.UPDATE_AVAILABLE, onDismiss = {})
}
