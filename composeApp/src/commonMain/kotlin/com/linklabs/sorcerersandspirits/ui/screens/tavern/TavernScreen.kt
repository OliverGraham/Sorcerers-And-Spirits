package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
internal data object TavernScreenRoute

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiStateFlow.collectAsState()
    when (val state = uiState) {
        UiState.Error -> {
        }
        is UiState.Loaded ->
            TavernScreen(
                tavern = state.data,
                onActionButtonClick = viewModel::onActionButtonClick,
                modifier = modifier,
            )
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernScreen(
    tavern: Tavern,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Sorcerers & Spirits",
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
    ) { padding ->
        ScaffoldContent(
            tavern = tavern,
            padding = padding,
            onActionButtonClick = onActionButtonClick,
        )
    }
}

@Composable
private fun ScaffoldContent(
    tavern: Tavern,
    padding: PaddingValues,
    onActionButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues = padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ClassCards()
        ActionButton(onActionButtonClick = onActionButtonClick)
        Text(text = "Travelers in Tavern:")
        tavern.travelers.forEach {
            Text(text = it.displayName)
        }
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
private fun ActionButton(
    onActionButtonClick: () -> Unit,
) {
    Button(
        onClick = onActionButtonClick,
    ) {
        Text(text = "Tap Button To Change Your Traveler Name")
    }
}
