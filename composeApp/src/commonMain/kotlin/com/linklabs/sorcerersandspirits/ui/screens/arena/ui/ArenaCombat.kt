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
import androidx.compose.material3.Surface
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
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.SharedTransitionProvider
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassCard
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.sharedBounds
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.diceCount
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isImpersonateRedundant
import com.linklabs.sorcerersandspirits.ui.screens.arena.ui.dice.DiceTray
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

private const val TRANSITION_LABEL = "ArenaCombatTransition"
private const val GRID_CARDS_KEY = "GridCardsKey"
private const val ABILITY_CONTROLS_KEY = "AbilityControlsKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ArenaState.Combat.ArenaCombat(modifier: Modifier = Modifier) {
    // Target selection is hoisted here so both the opponent grid (inside the per-turn content) and
    // the persistent dice tray (the roll control) share one source of truth.
    var selectedOpponentUserId by remember(key1 = activeUserIds) { mutableStateOf(value = "") }
    var isTargetLockedOptimistically by
        remember(key1 = activeUserIds) { mutableStateOf(value = false) }

    // Once the user commits an Armor-class roll, the Domain pins the defender (lockedTargetUserId);
    // the UI then shows that locked opponent and stops offering others, so the target survives the
    // intervening defensive-ability roll-offs. The optimistic flag locks the instant the user
    // rolls,
    // before the pinned state arrives.
    // With a single opponent and only a normal attack to make, there is no target to pick, so the
    // lone opponent is pre-selected; an explicit tap still takes precedence (SS-101).
    val targetedOpponentUserId =
        lockedTargetUserId
            ?: selectedOpponentUserId.ifEmpty { autoTargetedOpponentUserId.orEmpty() }
    val isTargetLocked = lockedTargetUserId != null || isTargetLockedOptimistically

    val onAttack = ArenaActionsProvider.current.onAttack
    val onUseAbility = ArenaActionsProvider.current.onUseAbility
    val lockTarget = { isTargetLockedOptimistically = true }

    // The dice are the only way to roll: one control drives them (the normal attack, or the single
    // ability roll-off when there's no attack); everything else stays a button below.
    val primaryRollControl = primaryRollControl()
    val secondaryControls = combatControls.filterNot { it == primaryRollControl }

    // The die count of the roll the dice gesture commits, so while that roll is in flight the tray
    // tumbles the right number of dice for it rather than the previous roll's shape (SS-117).
    val expectedDiceCount = primaryRollControl?.diceCount

    val isTargetMissing =
        primaryRollControl?.requiresTarget == true && targetedOpponentUserId.isEmpty()
    val onPrimaryRoll: (() -> Unit)? =
        if (primaryRollControl != null && primaryRollControl.enabled && !isTargetMissing) {
            rollActionFor(
                control = primaryRollControl,
                targetedOpponentUserId = targetedOpponentUserId,
                onAttack = onAttack,
                onUseAbility = onUseAbility,
                lockTarget = lockTarget,
            )
        } else {
            null
        }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = activeUserIds,
                label = TRANSITION_LABEL,
                modifier = Modifier.weight(weight = 1f),
            ) { _ ->
                SharedTransitionProvider(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                ) {
                    CombatContent(
                        targetedOpponentUserId = targetedOpponentUserId,
                        isTargetLocked = isTargetLocked,
                        onSelectTarget = { opponentId -> selectedOpponentUserId = opponentId },
                        secondaryControls = secondaryControls,
                        onAttack = onAttack,
                        onUseAbility = onUseAbility,
                        lockTarget = lockTarget,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            DiceTray(
                onRoll = onPrimaryRoll,
                needsTargetHint = isTargetMissing,
                expectedDiceCount = expectedDiceCount,
            )
            UserStatsRow(userPlayer = userPlayer)
        }
    }
}

/**
 * The single control the dice gesture rolls: the normal attack when it's available, otherwise the
 * lone ability roll-off. Multi-choice steps (e.g. Mirror Image's reroll options) have no single
 * roll, so the dice stay display-only and every option shows as a button.
 */
private fun ArenaState.Combat.primaryRollControl(): ArenaCombatControl? =
    combatControls.firstOrNull { it is ArenaCombatControl.NormalAttack }
        ?: combatControls.filter { it.diceCount != null }.singleOrNull()

@Composable
private fun ArenaState.Combat.CombatContent(
    targetedOpponentUserId: String,
    isTargetLocked: Boolean,
    onSelectTarget: (String) -> Unit,
    secondaryControls: List<ArenaCombatControl>,
    onAttack: (AttackArguments) -> Unit,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onUpdateTargetedOpponentId = onSelectTarget,
            )
        }

        if (secondaryControls.isNotEmpty()) {
            item(key = ABILITY_CONTROLS_KEY) {
                AbilityControls(
                    controls = secondaryControls,
                    targetedOpponentUserId = targetedOpponentUserId,
                    onAttack = onAttack,
                    onUseAbility = onUseAbility,
                    lockTarget = lockTarget,
                )
            }
        }
    }
}

@Composable
private fun UserStatsRow(userPlayer: PlayerState.Selected, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val characterClass = userPlayer.characterClass

            StatItem(label = "Class", value = characterClass.className)
            StatItem(label = "AC", value = characterClass.armorClass.toString())
            StatItem(label = "HP", value = characterClass.hitPointsDisplay)
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
    // A knocked-out opponent stays on screen but is skipped in combat, so its card must not be
    // targetable — otherwise the user could select and attack a knocked-out player (SS-123).
    val isOpponentTargetable =
        isItThisUsersTurn && !isTargetLocked && opponent.characterClass.isConscious

    CharacterClassCard(
        characterClass = opponent.characterClass,
        isActive = isActive,
        isSelected = targetedOpponentUserId == opponent.userId,
        enabled = isOpponentTargetable,
        selectedBy = opponent.displayName,
        modifier = modifier.sharedBounds(key = opponent.userId),
        onClick = { onUpdateTargetedOpponentId(opponent.userId) },
    )
}

/**
 * The actions the user can take instead of the dice roll — class abilities and roll-off choices —
 * each a button that fires immediately; the dice tray then animates whatever roll results.
 */
@Composable
private fun ArenaState.Combat.AbilityControls(
    controls: List<ArenaCombatControl>,
    targetedOpponentUserId: String,
    onAttack: (AttackArguments) -> Unit,
    onUseAbility: (UseAbilityArguments) -> Unit,
    lockTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        controls.forEach { control ->
            val hasRequiredTarget = !control.requiresTarget || targetedOpponentUserId.isNotEmpty()
            val isRedundantImpersonate =
                isImpersonateRedundant(
                    control = control,
                    targetedOpponentUserId = targetedOpponentUserId,
                )
            val isEnabled = control.enabled && hasRequiredTarget && !isRedundantImpersonate

            val onClick =
                rollActionFor(
                    control = control,
                    targetedOpponentUserId = targetedOpponentUserId,
                    onAttack = onAttack,
                    onUseAbility = onUseAbility,
                    lockTarget = lockTarget,
                )

            Button(onClick = onClick, enabled = isEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(text = control.label)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaCombatYourTurnPreview() {
    SorcerersAndSpiritsTheme {
        val rogue = previewCharacterClassState(type = CharacterClassType.Rogue)
        val mage = previewCharacterClassState(type = CharacterClassType.Mage)
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
        val rogue = previewCharacterClassState(type = CharacterClassType.Rogue)
        val mage = previewCharacterClassState(type = CharacterClassType.Mage)
        val paladin = previewCharacterClassState(type = CharacterClassType.Paladin)
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
