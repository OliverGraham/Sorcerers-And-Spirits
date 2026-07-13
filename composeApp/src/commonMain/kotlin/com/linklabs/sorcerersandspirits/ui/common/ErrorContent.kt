package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.error_generic_message
import sorcerersandspirits.composeapp.generated.resources.retry_button

@Composable
internal fun ErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(resource = Res.string.error_generic_message),
) {
    Column(
        modifier = modifier.fillMaxSize().padding(all = 24.dp),
        verticalArrangement =
            Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        AppButton(onClick = onRetry) {
            Text(text = stringResource(resource = Res.string.retry_button))
        }
    }
}

@Preview
@Composable
private fun ErrorContentPreview() {
    SorcerersAndSpiritsTheme { ErrorContent(onRetry = {}) }
}
