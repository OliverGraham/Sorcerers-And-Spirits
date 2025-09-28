package com.linklabs.ui.screens.tavern

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
internal data object TavernScreenRoute

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    TavernScreen(modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernScreen(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(modifier = Modifier.fillMaxWidth(), text = "Sorcerers & Spirits", textAlign = TextAlign.Center)
                },
            )
        },
    ) { padding ->
        ScaffoldContent(
            padding = padding,
        )
    }
}

@Composable
private fun ScaffoldContent(
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        ClassCards()
        ActionButton()
    }
}

@Composable
private fun ClassCards() {
    val classNames = listOf(
        "Rogue",
        "Duelist",
        "Mage",
        "Paladin",
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items = classNames) { className ->
            Card(
                modifier = Modifier.size(size = 150.dp),
            ) {
                Text(text = className)
            }
        }
    }
}

@Composable
private fun ActionButton() {
    /*
    Button(
        onClick = {},

    )
     */
}
