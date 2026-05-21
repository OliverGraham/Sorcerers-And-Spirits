package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCard
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.RollDiceArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO Arena: Lots of slop to update

private const val ARENA_COMBAT_TRANSITION_LABEL = "ArenaCombatTransition"
private const val GRID_CARDS_KEY = "GridCardsKey"
private const val ROLL_DICE_KEY = "RollDiceKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArenaState.Combat.ArenaCombat(modifier: Modifier = Modifier) {
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(targetState = activePlayerUserId, label = ARENA_COMBAT_TRANSITION_LABEL) { _
            ->
            SharedTransitionProvider(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@AnimatedContent,
            ) {
                CombatContent()
            }
        }
    }
}

@Composable
private fun ArenaState.Combat.CombatContent(modifier: Modifier = Modifier) {
    var rollDiceArguments by remember { mutableStateOf<RollDiceArguments?>(value = null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = activePlayerUserId) {
            ActivePlayerCard(
                activePlayer = playerMap.getValue(key = activePlayerUserId),
                isItThisUsersTurn = isItThisUsersTurn,
            )
        }

        item(key = GRID_CARDS_KEY) {
            GridCards(
                otherPlayers = otherPlayers.values.toList(),
                userPlayerId = userPlayerId,
                isItThisUsersTurn = isItThisUsersTurn,
                targetedOpponentId = rollDiceArguments?.opponentUserId,
                onUpdateTargetedOpponentId = { opponentId ->
                    rollDiceArguments = rollDiceArguments?.copy(opponentUserId = opponentId)
                },
            )
        }

        if (isItThisUsersTurn) {
            item(key = ROLL_DICE_KEY) {
                RollDiceSection(otherPlayers = otherPlayers, rollDiceArguments = rollDiceArguments)
            }
        }
    }
}

@Composable
private fun ActivePlayerCard(
    activePlayer: PlayerState.Selected,
    isItThisUsersTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    CharacterClassCard(
        characterClass = activePlayer.characterClass,
        isActive = true,
        isSelected = isItThisUsersTurn,
        selectedBy = if (isItThisUsersTurn) "You" else activePlayer.displayName,
        onClick = { /* Could show details */ },
        modifier =
            modifier.fillMaxWidth().height(height = 220.dp).sharedBounds(key = activePlayer.userId),
    )
}

@Composable
private fun GridCards(
    otherPlayers: List<PlayerState.Selected>,
    userPlayerId: String,
    isItThisUsersTurn: Boolean,
    targetedOpponentId: String?,
    onUpdateTargetedOpponentId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(count = 2),
        modifier = modifier.fillMaxWidth().heightIn(max = 1000.dp),
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        items(items = otherPlayers, key = { it.userId }) { otherPlayer ->
            GridPlayerCard(
                otherPlayer = otherPlayer,
                isUser = otherPlayer.userId == userPlayerId,
                isItThisUsersTurn = isItThisUsersTurn,
                targetedOpponentId = targetedOpponentId,
                onUpdateTargetedOpponentId = onUpdateTargetedOpponentId,
            )
        }
    }
}

@Composable
private fun GridPlayerCard(
    otherPlayer: PlayerState.Selected,
    isUser: Boolean,
    isItThisUsersTurn: Boolean,
    targetedOpponentId: String?,
    onUpdateTargetedOpponentId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTargeted = otherPlayer.userId == targetedOpponentId

    CharacterClassCard(
        characterClass = otherPlayer.characterClass,
        isSelected = isTargeted,
        enabled = isItThisUsersTurn && targetedOpponentId == null && !isUser,
        selectedBy = otherPlayer.displayName,
        modifier = modifier.sharedBounds(key = otherPlayer.userId),
        onClick = { onUpdateTargetedOpponentId(otherPlayer.userId) },
    )
}

@Composable
private fun RollDiceSection(
    otherPlayers: Map<UserId, PlayerState.Selected>,
    rollDiceArguments: RollDiceArguments?,
) {
    // TODO: Figure out correct way
    val buttonText = "Roll Dice"

    /*
    when (pendingRollType) {
        RollType.ArmorClass ->
            "Roll to Hit (AC: ${otherPlayers[targetedOpponentId]?.characterClass?.armorClass ?: "?"})"

        RollType.Health ->
            "Roll for KILL! (HP: ${otherPlayers[targetedOpponentId]?.characterClass?.hitPoints ?: "?"})"

        else -> "Roll Dice"
    }
     */

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (rollDiceArguments?.opponentUserId == null) {
            Text(
                text = "Tap an enemy card to select your target",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        val onRollDice = ArenaActionsProvider.current.onRollDice
        Button(
            onClick = { onRollDice },
            enabled = rollDiceArguments?.opponentUserId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = buttonText)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaCombatYourTurnPreview() {
    SorcerersAndSpiritsTheme {
        val rogue = CharacterClass.getRogue().toState()
        val mage = CharacterClass.getMage().toState()
        val userPlayer =
            PlayerState.Selected(userId = "1", displayName = "User", characterClass = rogue)
        val otherPlayer =
            PlayerState.Selected(userId = "2", displayName = "Enemy", characterClass = mage)
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaCombatEnemyTurnPreview() {
    SorcerersAndSpiritsTheme {
        val rogue = CharacterClass.getRogue().toState()
        val mage = CharacterClass.getMage().toState()
        val paladin = CharacterClass.getPaladin().toState()
        val userPlayer =
            PlayerState.Selected(userId = "1", displayName = "User", characterClass = rogue)
        val enemy1 =
            PlayerState.Selected(userId = "2", displayName = "Enemy 1", characterClass = mage)
        val enemy2 =
            PlayerState.Selected(userId = "3", displayName = "Enemy 2", characterClass = paladin)
    }
}
