package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments
import com.linklabs.sorcerersandspirits.ui.screens.arena.UseAbilityArguments

@Immutable
sealed interface CombatActionState {
    val activeUserIds: Set<String>

    @Immutable data class Idle(override val activeUserIds: Set<String>) : CombatActionState

    @Immutable
    data class NormalAttack(
        val roll: NormalAttackType,
        val attackTurn: AttackTurn,
        val defenderUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        val attackTurn: AttackTurn,
        val defenderUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState
}

@Immutable
sealed interface AbilityActionState {

    @Immutable
    sealed interface Activated : AbilityActionState {
        @Immutable data class AllIn(val allInAbility: OffensiveAbility.AllIn) : Activated

        @Immutable
        data class SummonMinion(val summonMinionAbility: OffensiveAbility.SummonMinion) : Activated

        @Immutable data object PrepareAttack : Activated

        @Immutable
        data class Impersonate(val impersonateAbility: OffensiveAbility.Impersonate) : Activated
    }

    @Immutable
    sealed interface Passive : AbilityActionState {

        @Immutable
        sealed interface LuckyCoin : Passive {
            val attackerRollTotal: Int

            @Immutable data class Flip(override val attackerRollTotal: Int) : LuckyCoin

            @Immutable data class HighRoller(override val attackerRollTotal: Int) : LuckyCoin
        }

        @Immutable data class Riposte(val attackerRollTotal: Int) : Passive

        @Immutable
        sealed interface Persuade : Passive {
            @Immutable data object StrumLute : Persuade

            @Immutable
            data class Charm(
                val targetableUserIds: Set<String>,
                val selectedCharmTargetUserId: String?,
            ) : Persuade
        }

        @Immutable data object Encore : Passive

        @Immutable
        data class Teleport(
            val attackerRoll: TeleportDieState,
            val defenderRoll: TeleportDieState,
            val attackerDoublesBonus: Int,
        ) : Passive

        @Immutable
        data class TeleportDieState(
            val userId: String,
            val diePosition: DiePosition,
            val roll: Int? = null,
        )

        @Immutable
        data class MirrorImage(
            val firstDie: MirrorImageDieState,
            val secondDie: MirrorImageDieState,
            val attackType: NormalAttackType,
            val existingDoublesBonus: Int,
            val mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
            val mirrorImageRerollSelection: MirrorImageRerollSelection =
                MirrorImageRerollSelection.None,
        ) : Passive

        @Immutable data class MirrorImageDieState(val originalRoll: Int, val reroll: Int? = null)

        @Immutable
        sealed interface HolyShield : Passive {
            @Immutable data object Prayer : HolyShield

            @Immutable data object Wrath : HolyShield
        }
    }
}

internal val AbilityActionState.requiresTarget: Boolean
    get() =
        when (this) {
            is AbilityActionState.Activated.PrepareAttack,
            is AbilityActionState.Activated.Impersonate,
            is AbilityActionState.Passive.Persuade.Charm -> true

            else -> false
        }

// TODO: What is going on here? Refactor right? One when statement necessary?
internal fun AttackArguments.toDomain(): CurrentCombatAction.NormalAttack? =
    when (combatActionState) {
        is CombatActionState.AbilityRoll -> null

        is CombatActionState.Idle ->
            CurrentCombatAction.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackTurn = AttackTurn.Standard(attackerUserId = userPlayerId),
                defenderUserId = targetedOpponentUserId,
            )


        is CombatActionState.NormalAttack ->
            CurrentCombatAction.NormalAttack(
                roll = combatActionState.roll,
                attackTurn = combatActionState.attackTurn,
                defenderUserId = combatActionState.defenderUserId,
            )
    }


// TODO: What is going on here?
internal fun UseAbilityArguments.toDomain(): CurrentCombatAction.AbilityRoll? {
    val purpose = abilityActionState.withSelectedCharmTarget(targetUserId = targetedOpponentUserId)

    return when (combatActionState) {
        is CombatActionState.AbilityRoll -> {
            val isResponseToOwedStep = combatActionState.action::class == abilityActionState::class
            if (isResponseToOwedStep) {
                CurrentCombatAction.AbilityRoll(
                    purpose = purpose,
                    attackTurn = combatActionState.attackTurn,
                    defenderUserId = combatActionState.defenderUserId,
                    rollingUserId = userPlayerId,
                )
            } else {
                null
            }
        }

        is CombatActionState.Idle -> {
            val isUsersTurn = userPlayerId in combatActionState.activeUserIds
            val isActivatedAbility = abilityActionState is AbilityActionState.Activated
            val isActivationOfUsersTurn = isUsersTurn && isActivatedAbility
            if (isActivationOfUsersTurn) {
                CurrentCombatAction.AbilityRoll(
                    purpose = purpose,
                    attackTurn = AttackTurn.Standard(attackerUserId = userPlayerId),
                    defenderUserId = targetedOpponentUserId.ifEmpty { userPlayerId },
                    rollingUserId = userPlayerId,
                )
            } else {
                null
            }
        }

        is CombatActionState.NormalAttack -> null
    }
}

/**
 * Resolves the Bard's chosen Charm target into the domain ability. Only Persuade's Charm consumes a
 * target, and only a [targetUserId] the Domain already vetted as charmable (a conscious opponent
 * that is neither the Bard nor the attacker, per
 * [AbilityActionState.Passive.Persuade.Charm.targetableUserIds]) is accepted; any other input
 * leaves the target unset. Every other ability ignores [targetUserId].
 */
private fun AbilityActionState.withSelectedCharmTarget(targetUserId: String): AbilityAction =
    if (this is AbilityActionState.Passive.Persuade.Charm && targetUserId in targetableUserIds) {
        copy(selectedCharmTargetUserId = targetUserId).toDomain()
    } else {
        toDomain()
    }

internal fun AbilityActionState.toDomain(): AbilityAction =
    when (this) {
        is AbilityActionState.Activated ->
            when (this) {
                is AbilityActionState.Activated.AllIn ->
                    AbilityAction.Activated.AllIn(allInAbility = allInAbility)

                is AbilityActionState.Activated.SummonMinion ->
                    AbilityAction.Activated.SummonMinion(summonMinionAbility = summonMinionAbility)

                AbilityActionState.Activated.PrepareAttack -> AbilityAction.Activated.PrepareAttack

                is AbilityActionState.Activated.Impersonate ->
                    AbilityAction.Activated.Impersonate(impersonateAbility = impersonateAbility)
            }

        is AbilityActionState.Passive ->
            when (this) {
                is AbilityActionState.Passive.LuckyCoin ->
                    when (this) {
                        is AbilityActionState.Passive.LuckyCoin.Flip ->
                            AbilityAction.Passive.LuckyCoin.Flip(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityActionState.Passive.LuckyCoin.HighRoller ->
                            AbilityAction.Passive.LuckyCoin.HighRoller(
                                attackerRollTotal = attackerRollTotal
                            )
                    }

                is AbilityActionState.Passive.Riposte ->
                    AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityActionState.Passive.Persuade ->
                    when (this) {
                        AbilityActionState.Passive.Persuade.StrumLute ->
                            AbilityAction.Passive.Persuade.StrumLute

                        is AbilityActionState.Passive.Persuade.Charm ->
                            AbilityAction.Passive.Persuade.Charm(
                                targetableUserIds = targetableUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityActionState.Passive.Encore -> AbilityAction.Passive.Encore

                is AbilityActionState.Passive.Teleport ->
                    AbilityAction.Passive.Teleport(
                        attackerRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = attackerRoll.userId,
                                diePosition = attackerRoll.diePosition,
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = defenderRoll.userId,
                                diePosition = defenderRoll.diePosition,
                                roll = defenderRoll.roll,
                            ),
                        attackerDoublesBonus = attackerDoublesBonus,
                    )

                is AbilityActionState.Passive.MirrorImage ->
                    AbilityAction.Passive.MirrorImage(
                        firstDie =
                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                originalRoll = firstDie.originalRoll,
                                reroll = firstDie.reroll,
                            ),
                        secondDie =
                            AbilityAction.Passive.MirrorImage.MirrorImageDie(
                                originalRoll = secondDie.originalRoll,
                                reroll = secondDie.reroll,
                            ),
                        attackType = attackType,
                        existingDoublesBonus = existingDoublesBonus,
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
                        mirrorImageRerollSelection = mirrorImageRerollSelection,
                    )

                is AbilityActionState.Passive.HolyShield ->
                    when (this) {
                        AbilityActionState.Passive.HolyShield.Prayer ->
                            AbilityAction.Passive.HolyShield.Prayer

                        AbilityActionState.Passive.HolyShield.Wrath ->
                            AbilityAction.Passive.HolyShield.Wrath
                    }
            }
    }

internal fun NextCombatAction.toCombatActionState(): CombatActionState =
    when (this) {
        is NextCombatAction.Idle -> CombatActionState.Idle(activeUserIds = activeUserIds)

        is NextCombatAction.NormalAttack ->
            CombatActionState.NormalAttack(
                roll = roll,
                attackTurn = attackTurn,
                defenderUserId = defenderUserId,
                activeUserIds = activeUserIds,
            )

        is NextCombatAction.AbilityRoll ->
            CombatActionState.AbilityRoll(
                action = purpose.toState(),
                attackTurn = attackTurn,
                defenderUserId = defenderUserId,
                activeUserIds = activeUserIds,
            )
    }

internal fun AbilityAction.toState(): AbilityActionState =
    when (this) {
        is AbilityAction.Activated ->
            when (this) {
                is AbilityAction.Activated.AllIn ->
                    AbilityActionState.Activated.AllIn(allInAbility = allInAbility)

                is AbilityAction.Activated.SummonMinion ->
                    AbilityActionState.Activated.SummonMinion(
                        summonMinionAbility = summonMinionAbility
                    )

                AbilityAction.Activated.PrepareAttack -> AbilityActionState.Activated.PrepareAttack

                is AbilityAction.Activated.Impersonate ->
                    AbilityActionState.Activated.Impersonate(
                        impersonateAbility = impersonateAbility
                    )
            }

        is AbilityAction.Passive ->
            when (this) {
                is AbilityAction.Passive.LuckyCoin ->
                    when (this) {
                        is AbilityAction.Passive.LuckyCoin.Flip ->
                            AbilityActionState.Passive.LuckyCoin.Flip(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityAction.Passive.LuckyCoin.HighRoller ->
                            AbilityActionState.Passive.LuckyCoin.HighRoller(
                                attackerRollTotal = attackerRollTotal
                            )
                    }

                is AbilityAction.Passive.Riposte ->
                    AbilityActionState.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityAction.Passive.Persuade ->
                    when (this) {
                        AbilityAction.Passive.Persuade.StrumLute ->
                            AbilityActionState.Passive.Persuade.StrumLute

                        is AbilityAction.Passive.Persuade.Charm ->
                            AbilityActionState.Passive.Persuade.Charm(
                                targetableUserIds = targetableUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityAction.Passive.Encore -> AbilityActionState.Passive.Encore

                is AbilityAction.Passive.Teleport ->
                    AbilityActionState.Passive.Teleport(
                        attackerRoll =
                            AbilityActionState.Passive.TeleportDieState(
                                userId = attackerRoll.userId,
                                diePosition = attackerRoll.diePosition,
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityActionState.Passive.TeleportDieState(
                                userId = defenderRoll.userId,
                                diePosition = defenderRoll.diePosition,
                                roll = defenderRoll.roll,
                            ),
                        attackerDoublesBonus = attackerDoublesBonus,
                    )

                is AbilityAction.Passive.MirrorImage ->
                    AbilityActionState.Passive.MirrorImage(
                        firstDie =
                            AbilityActionState.Passive.MirrorImageDieState(
                                originalRoll = firstDie.originalRoll,
                                reroll = firstDie.reroll,
                            ),
                        secondDie =
                            AbilityActionState.Passive.MirrorImageDieState(
                                originalRoll = secondDie.originalRoll,
                                reroll = secondDie.reroll,
                            ),
                        attackType = attackType,
                        existingDoublesBonus = existingDoublesBonus,
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
                        mirrorImageRerollSelection = mirrorImageRerollSelection,
                    )

                is AbilityAction.Passive.HolyShield ->
                    when (this) {
                        AbilityAction.Passive.HolyShield.Prayer ->
                            AbilityActionState.Passive.HolyShield.Prayer

                        AbilityAction.Passive.HolyShield.Wrath ->
                            AbilityActionState.Passive.HolyShield.Wrath
                    }
            }
    }
