package com.linklabs.sorcerersandspirits.ui.main.ui

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.app_name
import sorcerersandspirits.composeapp.generated.resources.ic_menu
import sorcerersandspirits.composeapp.generated.resources.open_drawer

// TODO: Remove this / figure out what should be at the top
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(resource = Res.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        actions = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_menu),
                    contentDescription = stringResource(resource = Res.string.open_drawer),
                )
            }
        },
    )
}

@Preview
@Composable
private fun TopBarPreview() {
    SorcerersAndSpiritsTheme { TopBar(onMenuClick = {}) }
}
