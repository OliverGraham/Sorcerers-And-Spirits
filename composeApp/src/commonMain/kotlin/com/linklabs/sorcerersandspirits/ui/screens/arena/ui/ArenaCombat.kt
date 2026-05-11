package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCard
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

@Composable
internal fun ArenaState.Combat.ArenaCombat(modifier: Modifier = Modifier) {
    val actions = ArenaActionsProvider.current

    Column(
        modifier = modifier.fillMaxSize().padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isItThisUsersTurn) "Your Turn!" else "${activePlayer.displayName}'s Turn",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Time Remaining: $secondsRemaining",
            style = MaterialTheme.typography.titleMedium,
            color =
                if (secondsRemaining <= 5) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(count = 2),
            modifier = Modifier.weight(weight = 1f),
            contentPadding = PaddingValues(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            item {
                CharacterClassCard(
                    characterClass = userPlayer.characterClass,
                    isSelected = activePlayer.userId == userPlayer.userId,
                    selectedBy = "You",
                    onClick = { /* Could show details */ },
                )
            }
            items(items = otherPlayers) { player ->
                val isTargeted = player.userId == pendingTargetUserId
                CharacterClassCard(
                    characterClass = player.characterClass,
                    isSelected = isTargeted || activePlayer.userId == player.userId,
                    enabled =
                        isItThisUsersTurn &&
                            pendingRollType ==
                                com.linklabs.sorcerersandspirits.domain.arena.model.RollType
                                    .ArmorClass,
                    selectedBy = if (isTargeted) "TARGETED" else player.displayName,
                    onClick = {
                        if (isItThisUsersTurn) {
                            actions.onAttack(player.userId)
                        }
                    },
                )
            }
        }

        if (isItThisUsersTurn) {
            val buttonText =
                when (pendingRollType) {
                    com.linklabs.sorcerersandspirits.domain.arena.model.RollType.ArmorClass ->
                        "Roll to Hit (AC: ${otherPlayers.find { it.userId == pendingTargetUserId }?.characterClass?.armorClass ?: "?"})"

                    com.linklabs.sorcerersandspirits.domain.arena.model.RollType.Health ->
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

                Button(
                    onClick = actions.onRollDice,
                    enabled = pendingTargetUserId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = buttonText)
                }
            }
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
                messages = emptyList(),
                unhandledMessagesCount = 0,
                secondsRemaining = 15,
                activePlayer = userPlayer,
                userPlayer = userPlayer,
                otherPlayers = listOf(otherPlayer),
                isItThisUsersTurn = true,
                pendingRollType =
                    com.linklabs.sorcerersandspirits.domain.arena.model.RollType.ArmorClass,
                pendingTargetUserId = null,
                turnOrder = listOf("1", "2"),
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
                messages = emptyList(),
                unhandledMessagesCount = 0,
                secondsRemaining = 8,
                activePlayer = otherPlayer,
                userPlayer = userPlayer,
                otherPlayers = listOf(otherPlayer),
                isItThisUsersTurn = false,
                pendingRollType =
                    com.linklabs.sorcerersandspirits.domain.arena.model.RollType.ArmorClass,
                pendingTargetUserId = null,
                turnOrder = listOf("1", "2"),
            )
            .ArenaCombat()
    }
}
