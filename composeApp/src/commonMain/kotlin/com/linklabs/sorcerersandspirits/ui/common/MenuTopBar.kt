package com.linklabs.sorcerersandspirits.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.main.navigation.LocalOpenDrawer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_menu
import sorcerersandspirits.composeapp.generated.resources.open_drawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MenuTopBar(title: String, modifier: Modifier = Modifier) {
    val onOpenDrawer = LocalOpenDrawer.current

    TopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_menu),
                    contentDescription = stringResource(resource = Res.string.open_drawer),
                    modifier = Modifier.size(size = 48.dp),
                )
            }
        },
    )
}
