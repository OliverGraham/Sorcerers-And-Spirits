package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCard
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.toState
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.diceCount
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

private const val TRANSITION_LABEL = "ArenaCombatTransition"
private const val GRID_CARDS_KEY = "GridCardsKey"
private const val ROLL_DICE_KEY = "RollDiceKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArenaState.Combat.ArenaCombat(modifier: Modifier = Modifier) {
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(targetState = activeUserIds, label = TRANSITION_LABEL) { _ ->
            SharedTransitionProvider(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@AnimatedContent,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CombatContent(modifier = Modifier.weight(1f))
                    UserStatsRow(userPlayer = userPlayer)
                }
            }
        }
    }
}

@Composable
private fun ArenaState.Combat.CombatContent(modifier: Modifier = Modifier) {
    var targetedOpponentUserId by remember(key1 = activeUserIds) { mutableStateOf(value = "") }
    var isTargetLocked by remember(key1 = activeUserIds) { mutableStateOf(value = false) }

    val opponents = playerMap.values.filter { it.userId != userPlayerId }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = GRID_CARDS_KEY) {
            GridCards(
                opponents = opponents,
                activeUserIds = activeUserIds,
                isItThisUsersTurn = isItThisUsersTurn,
                isTargetLocked = isTargetLocked,
                targetedOpponentUserId = targetedOpponentUserId,
                onUpdateTargetedOpponentId = { opponentId -> targetedOpponentUserId = opponentId },
            )
        }

        if (combatControls.isNotEmpty()) {
            item(key = ROLL_DICE_KEY) {
                RollDiceSection(
                    combatControls = combatControls,
                    targetedOpponentUserId = targetedOpponentUserId,
                    updateIsTargetLocked = { isTargetLocked = true },
                )
            }
        }
    }
}

@Composable
private fun UserStatsRow(userPlayer: PlayerState.Selected, modifier: Modifier = Modifier) {
    val characterClass = userPlayer.characterClass
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(label = "Class", value = characterClass.className)
            StatItem(label = "AC", value = characterClass.armorClass.toString())
            StatItem(label = "HP", value = characterClass.hitPoints.toString())
            StatItem(label = "Speed", value = characterClass.speed.toString())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GridCards(
    opponents: List<PlayerState.Selected>,
    activeUserIds: Set<String>,
    isItThisUsersTurn: Boolean,
    isTargetLocked: Boolean,
    targetedOpponentUserId: String?,
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
        items(items = opponents, key = { it.userId }) { opponent ->
            GridPlayerCard(
                opponent = opponent,
                isActive = opponent.userId in activeUserIds,
                isItThisUsersTurn = isItThisUsersTurn,
                isTargetLocked = isTargetLocked,
                targetedOpponentUserId = targetedOpponentUserId,
                onUpdateTargetedOpponentId = onUpdateTargetedOpponentId,
            )
        }
    }
}

@Composable
private fun GridPlayerCard(
    opponent: PlayerState.Selected,
    isActive: Boolean,
    isItThisUsersTurn: Boolean,
    isTargetLocked: Boolean,
    targetedOpponentUserId: String?,
    onUpdateTargetedOpponentId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CharacterClassCard(
        characterClass = opponent.characterClass,
        isActive = isActive,
        isSelected = targetedOpponentUserId == opponent.userId,
        enabled = isItThisUsersTurn && !isTargetLocked,
        selectedBy = opponent.displayName,
        modifier = modifier.sharedBounds(key = opponent.userId),
        onClick = { onUpdateTargetedOpponentId(opponent.userId) },
    )
}

@Composable
private fun ArenaState.Combat.RollDiceSection(
    combatControls: List<ArenaCombatControl>,
    targetedOpponentUserId: String,
    updateIsTargetLocked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (targetedOpponentUserId.isEmpty() && combatControls.any { it.requiresTarget }) {
            Text(
                text = "Tap an enemy card to select your target",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        combatControls.forEach { combatControl ->
            when (combatControl) {
                is ArenaCombatControl.NormalAttack ->
                    AttackControl(
                        combatActionState = combatActionState,
                        targetedOpponentUserId = targetedOpponentUserId,
                        updateIsTargetLocked = updateIsTargetLocked,
                        userPlayerId = userPlayerId,
                        requiresTarget = combatControl.requiresTarget,
                        diceCount = combatControl.diceCount,
                    )

                is ArenaCombatControl.ActivatedAbility ->
                    AbilityControl(
                        label = combatControl.label,
                        abilityActionState = combatControl.action,
                        requiresTarget = combatControl.requiresTarget,
                        enabled = combatControl.enabled,
                        targetedOpponentUserId = targetedOpponentUserId,
                        updateIsTargetLocked = updateIsTargetLocked,
                        userPlayerId = userPlayerId,
                        diceCount = combatControl.diceCount,
                    )

                is ArenaCombatControl.AbilityRoll ->
                    AbilityControl(
                        label = combatControl.label,
                        abilityActionState = combatControl.action,
                        requiresTarget = combatControl.requiresTarget,
                        enabled = combatControl.enabled,
                        targetedOpponentUserId = targetedOpponentUserId,
                        updateIsTargetLocked = updateIsTargetLocked,
                        userPlayerId = userPlayerId,
                        diceCount = combatControl.diceCount,
                    )
            }
        }
    }
}

@Composable
private fun AbilityControl(
    label: String,
    abilityActionState: AbilityActionState,
    requiresTarget: Boolean,
    enabled: Boolean,
    targetedOpponentUserId: String,
    updateIsTargetLocked: () -> Unit,
    userPlayerId: String,
    diceCount: Int?,
    modifier: Modifier = Modifier,
) {
    val onUseAbility = ArenaActionsProvider.current.onUseAbility
    val isEnabled = enabled && (!requiresTarget || targetedOpponentUserId.isNotEmpty())

    val performAbility = {
        if (requiresTarget) updateIsTargetLocked()

        val useAbilityArguments =
            UseAbilityArguments(
                abilityActionState = abilityActionState,
                targetedOpponentUserId = targetedOpponentUserId,
                userPlayerId = userPlayerId,
            )

        onUseAbility(useAbilityArguments)
    }

    if (diceCount != null) {
        HoldToRollDice(
            label = label,
            diceCount = diceCount,
            enabled = isEnabled,
            onRoll = performAbility,
            modifier = modifier,
        )
    } else {
        Button(onClick = performAbility, enabled = isEnabled, modifier = modifier.fillMaxWidth()) {
            Text(text = label)
        }
    }
}

@Composable
private fun AttackControl(
    combatActionState: CombatActionState,
    targetedOpponentUserId: String,
    updateIsTargetLocked: () -> Unit,
    userPlayerId: String,
    requiresTarget: Boolean,
    diceCount: Int?,
    modifier: Modifier = Modifier,
) {
    val onAttack = ArenaActionsProvider.current.onAttack
    val isEnabled = !requiresTarget || targetedOpponentUserId.isNotEmpty()

    val performAttack = {
        updateIsTargetLocked()

        val attackArguments =
            AttackArguments(
                combatActionState = combatActionState,
                targetedOpponentUserId = targetedOpponentUserId,
                userPlayerId = userPlayerId,
            )

        onAttack(attackArguments)
    }

    if (diceCount != null) {
        HoldToRollDice(
            label = "Attack",
            diceCount = diceCount,
            enabled = isEnabled,
            onRoll = performAttack,
            modifier = modifier,
        )
    } else {
        Button(onClick = performAttack, enabled = isEnabled, modifier = modifier.fillMaxWidth()) {
            Text(text = "Attack")
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
        val enemy = PlayerState.Selected(userId = "2", displayName = "Enemy", characterClass = mage)

        val combatState =
            ArenaState.Combat(
                playerMap = mapOf(userPlayer.userId to userPlayer, enemy.userId to enemy),
                userPlayerId = userPlayer.userId,
                combatActionState =
                    CombatActionState.NormalAttack(
                        roll = NormalAttackType.ArmorClass,
                        attackerUserId = userPlayer.userId,
                        defenderUserId = enemy.userId,
                        originalTurnUserId = userPlayer.userId,
                        activeUserIds = setOf(userPlayer.userId),
                    ),
                roundNumber = 1,
                isNewRound = true,
                doublesBonus = null,
            )

        combatState.ArenaCombat()
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

        val combatState =
            ArenaState.Combat(
                playerMap =
                    mapOf(
                        userPlayer.userId to userPlayer,
                        enemy1.userId to enemy1,
                        enemy2.userId to enemy2,
                    ),
                userPlayerId = userPlayer.userId,
                combatActionState =
                    CombatActionState.NormalAttack(
                        roll = NormalAttackType.ArmorClass,
                        attackerUserId = enemy1.userId,
                        defenderUserId = userPlayer.userId,
                        originalTurnUserId = enemy1.userId,
                        activeUserIds = setOf(enemy1.userId),
                    ),
                roundNumber = 1,
                isNewRound = true,
                doublesBonus = null,
            )

        combatState.ArenaCombat()
    }
}
