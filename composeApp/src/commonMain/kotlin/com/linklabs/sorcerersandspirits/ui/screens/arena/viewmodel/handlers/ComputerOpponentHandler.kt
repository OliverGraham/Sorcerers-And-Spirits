package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.AbilityActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaCombatControl
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMatchState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.CombatActionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.PlayerState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.deriveCombatControls
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.isUserTheRoller
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.requiresTarget
import kotlinx.coroutines.delay

internal const val COMPUTER_OPPONENT_TURN_DELAY_MILLIS = 3000L

internal sealed interface ComputerOpponentDecision {
    data class ChooseClass(
        val playerState: PlayerState,
        val characterClassState: CharacterClassState,
    ) : ComputerOpponentDecision

    data class Attack(val attackArguments: AttackArguments) : ComputerOpponentDecision

    data class UseAbility(val useAbilityArguments: UseAbilityArguments) : ComputerOpponentDecision
}

internal class ComputerOpponentHandler(
    private val computerUserId: String,
    private val turnDelayMillis: Long = COMPUTER_OPPONENT_TURN_DELAY_MILLIS,
) {

    fun owedActionKey(arenaMatchState: ArenaMatchState): String? = when (arenaMatchState) {
        is ArenaMatchState.ChoosingClasses ->
            if (arenaMatchState.activePlayerUserId == computerUserId) {
                "choose:${arenaMatchState.activePlayerUserId}"
            } else {
                null
            }

        is ArenaMatchState.Combat -> arenaMatchState.owedCombatKey()

        is ArenaMatchState.WaitingForAllPlayers,
        is ArenaMatchState.Finished -> null
    }

    suspend fun decideTurn(
        arenaMatchState: ArenaMatchState,
        characterClasses: List<CharacterClassState>,
    ): ComputerOpponentDecision? {
        if (owedActionKey(arenaMatchState = arenaMatchState) == null) return null

        delay(timeMillis = turnDelayMillis)

        return when (arenaMatchState) {
            is ArenaMatchState.ChoosingClasses ->
                decideClassChoice(
                    arenaMatchState = arenaMatchState,
                    characterClasses = characterClasses,
                )

            is ArenaMatchState.Combat -> decideCombatAction(arenaMatchState = arenaMatchState)

            is ArenaMatchState.WaitingForAllPlayers,
            is ArenaMatchState.Finished -> null
        }
    }

    private fun decideClassChoice(
        arenaMatchState: ArenaMatchState.ChoosingClasses,
        characterClasses: List<CharacterClassState>,
    ): ComputerOpponentDecision? {
        if (arenaMatchState.activePlayerUserId != computerUserId) return null

        val takenClassTypes =
            arenaMatchState.playerMap.values
                .filterIsInstance<PlayerState.Selected>()
                .map { it.characterClass.type }
        val availableClass =
            characterClasses.filterNot { it.type in takenClassTypes }.randomOrNull() ?: return null

        return ComputerOpponentDecision.ChooseClass(
            playerState = arenaMatchState.playerMap.getValue(key = computerUserId),
            characterClassState = availableClass,
        )
    }

    private fun decideCombatAction(
        arenaMatchState: ArenaMatchState.Combat,
    ): ComputerOpponentDecision? = when (val actionState = arenaMatchState.combatActionState) {
        is CombatActionState.Idle -> decideIdleTurn(arenaMatchState = arenaMatchState)

        is CombatActionState.NormalAttack ->
            ComputerOpponentDecision.Attack(
                attackArguments =
                AttackArguments(
                    targetedOpponentUserId = actionState.defenderUserId,
                    userPlayerId = computerUserId,
                    combatActionState = actionState,
                ),
            )

        is CombatActionState.AbilityRoll ->
            if (actionState.action.isUserTheRoller(userPlayerId = computerUserId)) {
                ComputerOpponentDecision.UseAbility(
                    useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = actionState.action,
                        targetedOpponentUserId = charmTargetOrEmpty(action = actionState.action),
                        userPlayerId = computerUserId,
                        combatActionState = actionState,
                    ),
                )
            } else {
                null
            }
    }

    private fun decideIdleTurn(arenaMatchState: ArenaMatchState.Combat): ComputerOpponentDecision? {
        val chosenControl =
            arenaMatchState
                .copy(userPlayerId = computerUserId)
                .deriveCombatControls()
                .randomOrNull() ?: return null

        return when (chosenControl) {
            is ArenaCombatControl.ActivatedAbility ->
                ComputerOpponentDecision.UseAbility(
                    useAbilityArguments =
                    UseAbilityArguments(
                        abilityActionState = chosenControl.action,
                        targetedOpponentUserId =
                        targetForActivatedAbility(
                            arenaMatchState = arenaMatchState,
                            action = chosenControl.action,
                        ),
                        userPlayerId = computerUserId,
                        combatActionState = arenaMatchState.combatActionState,
                    ),
                )

            is ArenaCombatControl.NormalAttack,
            is ArenaCombatControl.AbilityRoll -> {
                val targetUserId =
                    randomLivingOpponentUserId(arenaMatchState = arenaMatchState) ?: return null
                ComputerOpponentDecision.Attack(
                    attackArguments =
                    AttackArguments(
                        targetedOpponentUserId = targetUserId,
                        userPlayerId = computerUserId,
                        combatActionState = arenaMatchState.combatActionState,
                    ),
                )
            }
        }
    }

    private fun targetForActivatedAbility(
        arenaMatchState: ArenaMatchState.Combat,
        action: AbilityActionState.Activated,
    ): String = if (action.requiresTarget) {
        randomLivingOpponentUserId(arenaMatchState = arenaMatchState).orEmpty()
    } else {
        ""
    }

    private fun charmTargetOrEmpty(action: AbilityActionState): String = (action as? AbilityActionState.Passive.Persuade.Charm)
        ?.targetableUserIds
        ?.randomOrNull()
        .orEmpty()

    private fun randomLivingOpponentUserId(arenaMatchState: ArenaMatchState.Combat): String? = arenaMatchState.playerMap.values
        .filter { player ->
            player.userId != computerUserId && player.characterClass.hitPoints > 0
        }
        .randomOrNull()
        ?.userId

    private fun ArenaMatchState.Combat.owedCombatKey(): String? {
        if (isMatchOver) return null
        if (isActionInFlight) return null
        if (computerUserId !in combatActionState.activeUserIds) return null

        val action = combatActionState
        val isOwedRoll =
            action !is CombatActionState.AbilityRoll ||
                action.action.isUserTheRoller(userPlayerId = computerUserId)
        if (!isOwedRoll) return null

        return "combat:$roundNumber:$combatActionState"
    }
}
