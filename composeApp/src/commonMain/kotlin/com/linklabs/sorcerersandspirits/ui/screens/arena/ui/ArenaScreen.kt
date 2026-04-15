package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
internal fun ArenaScreen(
    arenaId: String,
    viewModel: ArenaScreenViewModel = koinInject { parametersOf(arenaId) },
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Arena: $arenaId")
        Button(onClick = onBack) { Text("Back") }
    }
}
