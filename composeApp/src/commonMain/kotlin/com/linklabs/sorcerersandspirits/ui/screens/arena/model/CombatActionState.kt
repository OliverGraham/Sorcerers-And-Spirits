package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.ui.screens.arena.AttackArguments

@Immutable
sealed interface CombatActionState {
    val activeUserIds: Set<String>

    @Immutable data class Idle(override val activeUserIds: Set<String>) : CombatActionState

    @Immutable
    data class NormalAttack(
        val roll: NormalAttackRollState,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
        override val activeUserIds: Set<String>,
    ) : CombatActionState
}

@Immutable
sealed interface NormalAttackRollState {

    @Immutable data object ArmorClass : NormalAttackRollState

    @Immutable data object Health : NormalAttackRollState
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
        ) : Passive

        @Immutable data class TeleportDieState(val userId: String, val roll: Int? = null)

        @Immutable
        data class MirrorImage(
            val firstDie: MirrorImageDieState,
            val secondDie: MirrorImageDieState,
            val attackType: NormalAttackType,
            val mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
        ) : Passive

        @Immutable data class MirrorImageDieState(val originalRoll: Int, val reroll: Int? = null)

        @Immutable
        sealed interface HolyShield : Passive {
            @Immutable data object Prayer : HolyShield

            @Immutable data object Wrath : HolyShield
        }
    }
}

/**
 * Maps the user's attack input to its domain counterpart. The combatant ids come from the current
 * [CombatActionState] when an attack is in progress, or from the user and their selected target
 * when starting a new attack from [CombatActionState.Idle].
 */
internal fun AttackArguments.toDomain(): CurrentCombatAction.NormalAttack? =
    when (combatActionState) {
        is CombatActionState.AbilityRoll -> null

        is CombatActionState.Idle ->
            CurrentCombatAction.NormalAttack(
                roll = NormalAttackRoll.ArmorClass,
                attackerUserId = userPlayerId,
                defenderUserId = targetedOpponentUserId,
                originalTurnUserId = userPlayerId,
            )

        is CombatActionState.NormalAttack ->
            CurrentCombatAction.NormalAttack(
                roll =
                    when (combatActionState.roll) {
                        NormalAttackRollState.ArmorClass -> NormalAttackRoll.ArmorClass
                        NormalAttackRollState.Health -> NormalAttackRoll.Health
                    },
                attackerUserId = combatActionState.attackerUserId,
                defenderUserId = combatActionState.defenderUserId,
                originalTurnUserId = combatActionState.originalTurnUserId,
            )
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
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityAction.Passive.Teleport.TeleportDie(
                                userId = defenderRoll.userId,
                                roll = defenderRoll.roll,
                            ),
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
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
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
                roll =
                    when (roll) {
                        NormalAttackRoll.ArmorClass -> NormalAttackRollState.ArmorClass
                        NormalAttackRoll.Health -> NormalAttackRollState.Health
                    },
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
                activeUserIds = activeUserIds,
            )

        is NextCombatAction.AbilityRoll ->
            CombatActionState.AbilityRoll(
                action = purpose.toState(),
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
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
                                roll = attackerRoll.roll,
                            ),
                        defenderRoll =
                            AbilityActionState.Passive.TeleportDieState(
                                userId = defenderRoll.userId,
                                roll = defenderRoll.roll,
                            ),
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
                        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
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
