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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCard
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

// TODO Arena: Lots of slop to update

private const val GRID_CARDS_KEY = "GridCardsKey"
private const val ROLL_DICE_KEY = "RollDiceKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArenaState.Combat.ArenaCombat(modifier: Modifier = Modifier) {
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(targetState = activePlayer.userId, label = "ArenaCombatTransition") { _ ->
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
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = activePlayer.userId) {
            ActivePlayerCard(activePlayer = activePlayer, isItThisUsersTurn = isItThisUsersTurn)
        }

        item(key = GRID_CARDS_KEY) {
            GridCards(
                otherPlayers = otherPlayers,
                isItThisUsersTurn = isItThisUsersTurn,
                pendingTargetUserId = pendingTargetUserId,
            )
        }

        if (isItThisUsersTurn) {
            item(key = ROLL_DICE_KEY) {
                RollDiceSection(
                    pendingRollType = pendingRollType,
                    otherPlayers = otherPlayers,
                    pendingTargetUserId = pendingTargetUserId,
                )
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
    isItThisUsersTurn: Boolean,
    pendingTargetUserId: String?,
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
                isItThisUsersTurn = isItThisUsersTurn,
                pendingTargetUserId = pendingTargetUserId,
            )
        }
    }
}

@Composable
private fun GridPlayerCard(
    otherPlayer: PlayerState.Selected,
    isItThisUsersTurn: Boolean,
    pendingTargetUserId: String?,
    modifier: Modifier = Modifier,
) {
    val isTargeted = otherPlayer.userId == pendingTargetUserId
    val onAttack = ArenaActionsProvider.current.onAttack

    CharacterClassCard(
        characterClass = otherPlayer.characterClass,
        isSelected = isTargeted,
        enabled = isItThisUsersTurn && pendingTargetUserId == null,
        selectedBy = if (isTargeted) "TARGETED" else otherPlayer.displayName,
        modifier = modifier.sharedBounds(key = otherPlayer.userId),
        onClick = {
            if (isItThisUsersTurn) {
                onAttack(otherPlayer.userId)
            }
        },
    )
}

@Composable
private fun RollDiceSection(
    pendingRollType: RollType?,
    otherPlayers: List<PlayerState.Selected>,
    pendingTargetUserId: String?,
) {
    val buttonText =
        when (pendingRollType) {
            RollType.ArmorClass ->
                "Roll to Hit (AC: ${otherPlayers.find { it.userId == pendingTargetUserId }?.characterClass?.armorClass ?: "?"})"

            RollType.Health ->
                "Roll for KILL! (HP: ${otherPlayers.find { it.userId == pendingTargetUserId }?.characterClass?.hitPoints ?: "?"})"

            else -> "Roll Dice"
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (pendingTargetUserId == null) {
            Text(
                text = "Tap an enemy card to select your target",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        val onRollDice = ArenaActionsProvider.current.onRollDice
        Button(
            onClick = onRollDice,
            enabled = pendingTargetUserId != null,
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

        ArenaState.Combat(
                activePlayer = userPlayer,
                userPlayer = userPlayer,
                otherPlayers = listOf(otherPlayer),
                isItThisUsersTurn = true,
                pendingRollType = RollType.ArmorClass,
                pendingTargetUserId = null,
            )
            .ArenaCombat()
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaCombatEnemyTurnPreview() {
    SorcerersAndSpiritsTheme {
        val rogue = CharacterClass.getRogue().toState()
        val mage = CharacterClass.getMage().toState()
        val userPlayer =
            PlayerState.Selected(userId = "1", displayName = "User", characterClass = rogue)
        val otherPlayer =
            PlayerState.Selected(userId = "2", displayName = "Enemy", characterClass = mage)

        ArenaState.Combat(
                activePlayer = otherPlayer,
                userPlayer = userPlayer,
                otherPlayers = listOf(otherPlayer),
                isItThisUsersTurn = false,
                pendingRollType = RollType.ArmorClass,
                pendingTargetUserId = null,
            )
            .ArenaCombat()
    }
}
