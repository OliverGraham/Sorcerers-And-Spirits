package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction

@Immutable
sealed interface NextCombatActionState {

    @Immutable data object Idle : NextCombatActionState

    @Immutable
    sealed interface NormalAttackRoll : NextCombatActionState {
        @Immutable data object ArmorClass : NormalAttackRoll

        @Immutable data object Health : NormalAttackRoll
    }

    @Immutable data class AbilityRoll(val action: AbilityActionState) : NextCombatActionState
}

@Immutable
sealed interface AbilityActionState {

    @Immutable
    sealed interface Activated : AbilityActionState {
        @Immutable data object AllIn : Activated

        @Immutable data object SummonMinion : Activated

        @Immutable data object PrepareAttack : Activated

        @Immutable data object Impersonate : Activated
    }

    @Immutable
    sealed interface Passive : AbilityActionState {

        @Immutable
        sealed interface Reaction : Passive {
            @Immutable data object Teleport : Reaction

            @Immutable data object Riposte : Reaction

            @Immutable data object Persuade : Reaction

            @Immutable data object LuckyCoin : Reaction

            @Immutable data object HolyShield : Reaction

            @Immutable
            data class MirrorImage(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : Reaction
        }

        @Immutable
        sealed interface Outcome : Passive {
            @Immutable data class Teleport(val defenderRoll: Int) : Outcome

            @Immutable
            sealed interface LuckyCoin : Outcome {
                @Immutable data object ArmorClass : LuckyCoin

                @Immutable data object Reflect : LuckyCoin
            }

            @Immutable
            sealed interface HolyShield : Outcome {
                @Immutable data object Survival : HolyShield

                @Immutable data object Reflect : HolyShield
            }

            @Immutable data object Encore : Outcome

            @Immutable
            data class MirrorImagePass(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : Outcome

            @Immutable
            data class MirrorImageReroll(
                val firstDie: Int,
                val secondDie: Int,
                val rerollFirstDie: Boolean,
                val triggeredByArmorRoll: Boolean,
            ) : Outcome
        }
    }
}

internal fun NextCombatAction.toState(): NextCombatActionState =
    when (this) {
        NextCombatAction.Idle -> NextCombatActionState.Idle

        NextCombatAction.NormalAttackRoll.ArmorClass ->
            NextCombatActionState.NormalAttackRoll.ArmorClass

        NextCombatAction.NormalAttackRoll.Health -> NextCombatActionState.NormalAttackRoll.Health

        is NextCombatAction.AbilityRoll ->
            NextCombatActionState.AbilityRoll(action = purpose.toState())
    }

internal fun AbilityRollPurpose.toState(): AbilityActionState =
    when (this) {
        is AbilityRollPurpose.Activated ->
            when (this) {
                is AbilityRollPurpose.Activated.AllIn -> AbilityActionState.Activated.AllIn

                is AbilityRollPurpose.Activated.SummonMinion ->
                    AbilityActionState.Activated.SummonMinion

                AbilityRollPurpose.Activated.PrepareAttack ->
                    AbilityActionState.Activated.PrepareAttack

                is AbilityRollPurpose.Activated.Impersonate ->
                    AbilityActionState.Activated.Impersonate
            }

        is AbilityRollPurpose.Passive.Reaction ->
            when (this) {
                AbilityRollPurpose.Passive.Reaction.Teleport ->
                    AbilityActionState.Passive.Reaction.Teleport

                AbilityRollPurpose.Passive.Reaction.Riposte ->
                    AbilityActionState.Passive.Reaction.Riposte

                AbilityRollPurpose.Passive.Reaction.Persuade ->
                    AbilityActionState.Passive.Reaction.Persuade

                AbilityRollPurpose.Passive.Reaction.LuckyCoin ->
                    AbilityActionState.Passive.Reaction.LuckyCoin

                AbilityRollPurpose.Passive.Reaction.HolyShield ->
                    AbilityActionState.Passive.Reaction.HolyShield

                is AbilityRollPurpose.Passive.Reaction.MirrorImage ->
                    AbilityActionState.Passive.Reaction.MirrorImage(
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
            }

        is AbilityRollPurpose.Passive.Outcome ->
            when (this) {
                is AbilityRollPurpose.Passive.Outcome.Teleport ->
                    AbilityActionState.Passive.Outcome.Teleport(defenderRoll = defenderRoll)

                AbilityRollPurpose.Passive.Outcome.LuckyCoin.ArmorClass ->
                    AbilityActionState.Passive.Outcome.LuckyCoin.ArmorClass

                AbilityRollPurpose.Passive.Outcome.LuckyCoin.Reflect ->
                    AbilityActionState.Passive.Outcome.LuckyCoin.Reflect

                AbilityRollPurpose.Passive.Outcome.HolyShield.Survival ->
                    AbilityActionState.Passive.Outcome.HolyShield.Survival

                AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect ->
                    AbilityActionState.Passive.Outcome.HolyShield.Reflect

                AbilityRollPurpose.Passive.Outcome.Encore ->
                    AbilityActionState.Passive.Outcome.Encore

                is AbilityRollPurpose.Passive.Outcome.MirrorImagePass ->
                    AbilityActionState.Passive.Outcome.MirrorImagePass(
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )

                is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll ->
                    AbilityActionState.Passive.Outcome.MirrorImageReroll(
                        firstDie = firstDie,
                        secondDie = secondDie,
                        rerollFirstDie = rerollFirstDie,
                        triggeredByArmorRoll = triggeredByArmorRoll,
                    )
            }
    }
