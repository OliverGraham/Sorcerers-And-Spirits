package com.linklabs.sorcerersandspirits.ui.screens.tavern

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.traveler.GamePlayStatus
import com.linklabs.sorcerersandspirits.ui.uistate.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_menu

@Serializable
internal data object TavernScreenRoute

@Composable
internal fun TavernScreen(
    viewModel: TavernScreenViewModel = koinInject(),
    onMenuClick: () -> Unit,
    onNavigateToArena: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    viewModel.eventChannelFlow.collectEvents(
        onNavigateToArena = onNavigateToArena,
    )

    val uiState by viewModel.uiStateFlow.collectAsState()
    when (val state = uiState) {
        UiState.Error -> {
        }
        is UiState.Loaded -> {
            val data = state.data
            TavernScreen(
                tavernState = data,
                onActionButtonClick = viewModel::onActionButtonClick,
                onMenuClick = onMenuClick,
                onSendChallengerNotifications = viewModel::onSendChallengerNotifications,
                modifier = modifier,
            )
            if (data.arenaChallenge != null) {
                ArenaChallengeDialog(
                    challenge = data.arenaChallenge,
                    userTraveler = data.userTraveler,
                    onAcceptChallenge = viewModel::onAcceptArenaChallenge,
                    onDeclineChallenge = viewModel::onDeclineArenaChallenge,
                )
            }
        }
        UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TavernScreen(
    tavernState: TavernState,
    onActionButtonClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSendChallengerNotifications: (TravelerState, Set<TravelerState>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    var selection by remember { mutableStateOf(setOf<TravelerState>()) }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 140.dp,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sorcerers & Spirits",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            painter = painterResource(resource = Res.drawable.ic_menu),
                            contentDescription = "Open Drawer",
                        )
                    }
                },
            )
        },
        sheetContent = {
            BottomSheetContent(
                travelerStates = tavernState.otherTravelers,
                selectedTravelers = selection,
                onTravelerClick = { traveler ->
                    selection = if (selection.contains(traveler)) {
                        selection - traveler
                    } else {
                        selection + traveler
                    }
                },
                onCompleteAction = {
                    onSendChallengerNotifications(tavernState.userTraveler, selection)
                    selection = emptySet()
                },
            )
        },
    ) { padding ->
        ScaffoldContent(
            padding = padding,
            onActionButtonClick = onActionButtonClick,
        )
    }
}

@Composable
private fun BottomSheetContent(
    travelerStates: List<TravelerState>,
    selectedTravelers: Set<TravelerState>,
    onTravelerClick: (TravelerState) -> Unit,
    onCompleteAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        AnimatedContent(
            targetState = travelerStates.size,
            modifier = Modifier.padding(top = 16.dp),
        ) { count ->
            Text(
                text = "Travelers in Tavern: $count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Challenge people to fight in The Arena:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        travelerStates.forEach { travelerState ->
            travelerState.ToCard(
                isSelected = selectedTravelers.contains(travelerState),
                onClick = { onTravelerClick(travelerState) },
            )
        }

        AnimatedVisibility(visible = selectedTravelers.isNotEmpty()) {
            Button(
                onClick = onCompleteAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(text = "Challenge (${selectedTravelers.size})")
            }
        }
    }
}

@Composable
private fun TravelerState.ToCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (isThisTravelerTheUser) "$displayName (You)" else displayName

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isThisTravelerTheUser -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            },
            contentColor = if (isThisTravelerTheUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = !isThisTravelerTheUser) { onClick() }
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isThisTravelerTheUser || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScaffoldContent(
    padding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") onActionButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues = padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO: What to put here?
    }
}

@Suppress("ComposableNaming")
@Composable
private fun Flow<TavernEvent>.collectEvents(
    onNavigateToArena: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        collect { event ->
            when (event) {
                is TavernEvent.NavigateToArena -> {
                    onNavigateToArena("")
                }
            }
        }
    }
}

@Composable
private fun ArenaChallengeDialog(
    challenge: ArenaChallengeState,
    userTraveler: TravelerState,
    onAcceptChallenge: (challenge: ArenaChallengeState, userTraveler: TravelerState) -> Unit,
    onDeclineChallenge: (challenge: ArenaChallengeState, userTraveler: TravelerState) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {
            onDeclineChallenge(challenge, userTraveler)
        },
        title = { Text("Arena Challenge") },
        text = { Text("${challenge.sender.displayName} has challenged you!") },
        confirmButton = {
            Button(onClick = { onAcceptChallenge(challenge, userTraveler) }) {
                Text("Accept (Test)")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDeclineChallenge(challenge, userTraveler) }) {
                Text("Decline")
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun TavernScreenPreview() {
    TavernScreen(
        tavernState = TavernState(
            userTraveler = TravelerState(id = "0", displayName = "It's a me", isThisTravelerTheUser = true, gamePlayStatus = GamePlayStatus.Available),
            otherTravelers = listOf(
                TravelerState(id = "1", displayName = "RandomName1", isThisTravelerTheUser = false, gamePlayStatus = GamePlayStatus.Available),
                TravelerState(id = "2", displayName = "RandomName2", isThisTravelerTheUser = false, gamePlayStatus = GamePlayStatus.Available),
            ),
        ),
        onActionButtonClick = {},
        onMenuClick = {},
        onSendChallengerNotifications = { _, _ -> },
    )
}
