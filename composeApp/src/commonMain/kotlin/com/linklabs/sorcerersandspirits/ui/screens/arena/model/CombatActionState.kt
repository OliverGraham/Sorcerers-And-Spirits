package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

@Immutable
sealed interface CombatActionState {
    val activeUserIds: Set<String>

    @Immutable data class Idle(override val activeUserIds: Set<String>) : CombatActionState

    @Immutable
    data class NormalAttack(
        val roll: NormalAttackRollState,
        override val activeUserIds: Set<String>,
    ) : CombatActionState

    @Immutable
    data class AbilityRoll(
        val action: AbilityActionState,
        override val activeUserIds: Set<String>,
    ) : CombatActionState
}

@Immutable
sealed interface NormalAttackRollState {
    @Immutable
    data class ArmorClass(
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
    ) : NormalAttackRollState

    @Immutable
    data class Health(val attackerUserId: String, val defenderUserId: String) :
        NormalAttackRollState
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
            @Immutable data class Flip(val attackerRollTotal: Int) : LuckyCoin

            @Immutable
            data class HighRoller(val attackerUserId: String, val attackerRollTotal: Int) :
                LuckyCoin
        }

        @Immutable data class Riposte(val attackerRollTotal: Int) : Passive

        @Immutable
        sealed interface Persuade : Passive {
            @Immutable data object StrumLute : Persuade

            @Immutable
            data class Charm(
                val attackerUserId: String,
                val targetableUserIds: Set<String>,
                val selectedCharmTargetUserId: String?,
            ) : Persuade
        }

        @Immutable data object Encore : Passive

        @Immutable
        data class Teleport(val attackerRoll: Int? = null, val defenderRoll: Int? = null) : Passive

        @Immutable
        data class MirrorImage(
            val firstDie: MirrorImageDieState,
            val secondDie: MirrorImageDieState,
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
 * The [CurrentCombatAction] produced when the active player taps the Attack button.
 *
 * Every class can always attack. A fresh turn ([CombatActionState.Idle]) opens with a to-hit roll
 * against the target's armor class; an in-progress [CombatActionState.NormalAttack] simply
 * continues whichever roll the domain says comes next (to-hit, or damage against health). Ability
 * rolls are not driven by the Attack button, so there is no normal attack to produce.
 */
internal fun CombatActionState.toNormalAttackAction(
    attackerUserId: String,
    defenderUserId: String,
): CurrentCombatAction.NormalAttack? {
    val rollState =
        when (this) {
            is CombatActionState.Idle ->
                NormalAttackRollState.ArmorClass(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    originalTurnUserId = attackerUserId,
                )

            is CombatActionState.NormalAttack -> roll

            is CombatActionState.AbilityRoll -> return null
        }

    val roll =
        when (rollState) {
            is NormalAttackRollState.ArmorClass ->
                NormalAttackRoll.ArmorClass(
                    attackerUserId = rollState.attackerUserId,
                    defenderUserId = rollState.defenderUserId,
                    originalTurnUserId = rollState.originalTurnUserId,
                )

            is NormalAttackRollState.Health ->
                NormalAttackRoll.Health(
                    attackerUserId = rollState.attackerUserId,
                    defenderUserId = rollState.defenderUserId,
                )
        }

    return CurrentCombatAction.NormalAttack(roll = roll)
}

internal fun NextCombatAction.toCombatActionState(): CombatActionState =
    when (this) {
        is NextCombatAction.Idle -> CombatActionState.Idle(activeUserIds = activeUserIds)

        is NextCombatAction.NormalAttack ->
            CombatActionState.NormalAttack(
                roll =
                    when (val roll = roll) {
                        is NormalAttackRoll.ArmorClass ->
                            NormalAttackRollState.ArmorClass(
                                attackerUserId = roll.attackerUserId,
                                defenderUserId = roll.defenderUserId,
                                originalTurnUserId = roll.originalTurnUserId,
                            )

                        is NormalAttackRoll.Health ->
                            NormalAttackRollState.Health(
                                attackerUserId = roll.attackerUserId,
                                defenderUserId = roll.defenderUserId,
                            )
                    },
                activeUserIds = activeUserIds,
            )

        is NextCombatAction.AbilityRoll ->
            CombatActionState.AbilityRoll(action = purpose.toState(), activeUserIds = activeUserIds)
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
                                attackerUserId = attackerUserId,
                                attackerRollTotal = attackerRollTotal,
                            )
                    }

                is AbilityAction.Passive.Riposte ->
                    AbilityActionState.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityAction.Passive.Persuade ->
                    when (this) {
                        is AbilityAction.Passive.Persuade.StrumLute ->
                            AbilityActionState.Passive.Persuade.StrumLute

                        is AbilityAction.Passive.Persuade.Charm ->
                            AbilityActionState.Passive.Persuade.Charm(
                                attackerUserId = attackerUserId,
                                targetableUserIds = targetableUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityAction.Passive.Encore -> AbilityActionState.Passive.Encore

                is AbilityAction.Passive.Teleport ->
                    AbilityActionState.Passive.Teleport(
                        attackerRoll = attackerRoll,
                        defenderRoll = defenderRoll,
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
