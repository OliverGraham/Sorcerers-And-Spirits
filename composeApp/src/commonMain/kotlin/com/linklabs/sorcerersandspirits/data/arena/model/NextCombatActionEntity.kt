package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.Serializable

@Serializable
sealed interface NextCombatActionEntity {
    @Serializable data object IdleEntity : NextCombatActionEntity

    @Serializable
    sealed interface NormalAttackRollEntity : NextCombatActionEntity {
        @Serializable data object ArmorClassEntity : NormalAttackRollEntity

        @Serializable data object HealthEntity : NormalAttackRollEntity
    }

    @Serializable
    data class AbilityRollEntity(val purpose: AbilityRollPurposeEntity) : NextCombatActionEntity
}

@Serializable
sealed interface AbilityRollPurposeEntity {

    @Serializable
    sealed interface ActivatedEntity : AbilityRollPurposeEntity {
        @Serializable
        data class AllInEntity(val allInAbility: OffensiveAbilityEntity.AllIn) : ActivatedEntity

        @Serializable
        data class SummonMinionEntity(
            val summonMinionAbility: OffensiveAbilityEntity.SummonMinion
        ) : ActivatedEntity

        @Serializable data object PrepareAttackEntity : ActivatedEntity

        @Serializable
        data class ImpersonateEntity(val impersonateAbility: OffensiveAbilityEntity.Impersonate) :
            ActivatedEntity
    }

    @Serializable
    sealed interface PassiveEntity : AbilityRollPurposeEntity {

        @Serializable
        sealed interface ReactionEntity : PassiveEntity {
            @Serializable data object TeleportEntity : ReactionEntity

            @Serializable data object RiposteEntity : ReactionEntity

            @Serializable data object PersuadeEntity : ReactionEntity

            @Serializable data object LuckyCoinEntity : ReactionEntity

            @Serializable data object HolyShieldEntity : ReactionEntity

            @Serializable
            data class MirrorImageEntity(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : ReactionEntity
        }

        @Serializable
        sealed interface OutcomeEntity : PassiveEntity {
            @Serializable data class TeleportEntity(val defenderRoll: Int) : OutcomeEntity

            @Serializable
            sealed interface LuckyCoinEntity : OutcomeEntity {
                @Serializable data object ArmorClassEntity : LuckyCoinEntity

                @Serializable data object ReflectEntity : LuckyCoinEntity
            }

            @Serializable
            sealed interface HolyShieldEntity : OutcomeEntity {
                @Serializable data object SurvivalEntity : HolyShieldEntity

                @Serializable data object ReflectEntity : HolyShieldEntity
            }

            @Serializable data object EncoreEntity : OutcomeEntity

            @Serializable
            data class MirrorImagePassEntity(
                val firstDie: Int,
                val secondDie: Int,
                val triggeredByArmorRoll: Boolean,
            ) : OutcomeEntity

            @Serializable
            data class MirrorImageRerollEntity(
                val firstDie: Int,
                val secondDie: Int,
                val rerollFirstDie: Boolean,
                val triggeredByArmorRoll: Boolean,
            ) : OutcomeEntity
        }
    }
}

internal fun NextCombatAction.toEntity(): NextCombatActionEntity =
    when (this) {
        NextCombatAction.Idle -> NextCombatActionEntity.IdleEntity

        NextCombatAction.NormalAttackRoll.ArmorClass ->
            NextCombatActionEntity.NormalAttackRollEntity.ArmorClassEntity

        NextCombatAction.NormalAttackRoll.Health ->
            NextCombatActionEntity.NormalAttackRollEntity.HealthEntity

        is NextCombatAction.AbilityRoll ->
            NextCombatActionEntity.AbilityRollEntity(purpose = purpose.toEntity())
    }

internal fun NextCombatActionEntity.toDomain(): NextCombatAction =
    when (this) {
        NextCombatActionEntity.IdleEntity -> NextCombatAction.Idle

        NextCombatActionEntity.NormalAttackRollEntity.ArmorClassEntity ->
            NextCombatAction.NormalAttackRoll.ArmorClass

        NextCombatActionEntity.NormalAttackRollEntity.HealthEntity ->
            NextCombatAction.NormalAttackRoll.Health

        is NextCombatActionEntity.AbilityRollEntity ->
            NextCombatAction.AbilityRoll(purpose = purpose.toDomain())
    }

internal fun AbilityRollPurpose.toEntity(): AbilityRollPurposeEntity =
    when (this) {
        is AbilityRollPurpose.Activated ->
            when (this) {
                is AbilityRollPurpose.Activated.AllIn ->
                    AbilityRollPurposeEntity.ActivatedEntity.AllInEntity(
                        allInAbility = allInAbility.toEntity() as OffensiveAbilityEntity.AllIn
                    )

                is AbilityRollPurpose.Activated.Impersonate ->
                    AbilityRollPurposeEntity.ActivatedEntity.ImpersonateEntity(
                        impersonateAbility =
                            impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate
                    )

                AbilityRollPurpose.Activated.PrepareAttack ->
                    AbilityRollPurposeEntity.ActivatedEntity.PrepareAttackEntity

                is AbilityRollPurpose.Activated.SummonMinion ->
                    AbilityRollPurposeEntity.ActivatedEntity.SummonMinionEntity(
                        summonMinionAbility =
                            summonMinionAbility.toEntity() as OffensiveAbilityEntity.SummonMinion
                    )
            }

        is AbilityRollPurpose.Passive ->
            when (this) {
                is AbilityRollPurpose.Passive.Outcome ->
                    when (this) {
                        AbilityRollPurpose.Passive.Outcome.Encore ->
                            AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.EncoreEntity

                        is AbilityRollPurpose.Passive.Outcome.HolyShield ->
                            when (this) {
                                AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect ->
                                    AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                        .HolyShieldEntity
                                        .ReflectEntity

                                AbilityRollPurpose.Passive.Outcome.HolyShield.Survival ->
                                    AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                        .HolyShieldEntity
                                        .SurvivalEntity
                            }

                        is AbilityRollPurpose.Passive.Outcome.LuckyCoin ->
                            when (this) {
                                AbilityRollPurpose.Passive.Outcome.LuckyCoin.ArmorClass ->
                                    AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                        .LuckyCoinEntity
                                        .ArmorClassEntity

                                AbilityRollPurpose.Passive.Outcome.LuckyCoin.Reflect ->
                                    AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                        .LuckyCoinEntity
                                        .ReflectEntity
                            }

                        is AbilityRollPurpose.Passive.Outcome.MirrorImagePass ->
                            AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                .MirrorImagePassEntity(
                                    firstDie = firstDie,
                                    secondDie = secondDie,
                                    triggeredByArmorRoll = triggeredByArmorRoll,
                                )

                        is AbilityRollPurpose.Passive.Outcome.MirrorImageReroll ->
                            AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                .MirrorImageRerollEntity(
                                    firstDie = firstDie,
                                    secondDie = secondDie,
                                    rerollFirstDie = rerollFirstDie,
                                    triggeredByArmorRoll = triggeredByArmorRoll,
                                )

                        is AbilityRollPurpose.Passive.Outcome.Teleport ->
                            AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.TeleportEntity(
                                defenderRoll = defenderRoll
                            )
                    }

                is AbilityRollPurpose.Passive.Reaction ->
                    when (this) {
                        AbilityRollPurpose.Passive.Reaction.HolyShield ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.HolyShieldEntity

                        AbilityRollPurpose.Passive.Reaction.LuckyCoin ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.LuckyCoinEntity

                        is AbilityRollPurpose.Passive.Reaction.MirrorImage ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.MirrorImageEntity(
                                firstDie = firstDie,
                                secondDie = secondDie,
                                triggeredByArmorRoll = triggeredByArmorRoll,
                            )

                        AbilityRollPurpose.Passive.Reaction.Persuade ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.PersuadeEntity

                        AbilityRollPurpose.Passive.Reaction.Riposte ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.RiposteEntity

                        AbilityRollPurpose.Passive.Reaction.Teleport ->
                            AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.TeleportEntity
                    }
            }
    }

// TODO: remove all casting in this file
internal fun AbilityRollPurposeEntity.toDomain(): AbilityRollPurpose =
    when (this) {
        is AbilityRollPurposeEntity.ActivatedEntity ->
            when (this) {
                is AbilityRollPurposeEntity.ActivatedEntity.AllInEntity ->
                    AbilityRollPurpose.Activated.AllIn(
                        allInAbility = allInAbility.toDomain() as OffensiveAbility.AllIn
                    )

                is AbilityRollPurposeEntity.ActivatedEntity.ImpersonateEntity ->
                    AbilityRollPurpose.Activated.Impersonate(
                        impersonateAbility =
                            impersonateAbility.toDomain() as OffensiveAbility.Impersonate
                    )

                AbilityRollPurposeEntity.ActivatedEntity.PrepareAttackEntity ->
                    AbilityRollPurpose.Activated.PrepareAttack

                is AbilityRollPurposeEntity.ActivatedEntity.SummonMinionEntity ->
                    AbilityRollPurpose.Activated.SummonMinion(
                        summonMinionAbility =
                            summonMinionAbility.toDomain() as OffensiveAbility.SummonMinion
                    )
            }

        is AbilityRollPurposeEntity.PassiveEntity ->
            when (this) {
                is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity ->
                    when (this) {
                        AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.EncoreEntity ->
                            AbilityRollPurpose.Passive.Outcome.Encore

                        is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.HolyShieldEntity ->
                            when (this) {
                                AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                    .HolyShieldEntity
                                    .ReflectEntity ->
                                    AbilityRollPurpose.Passive.Outcome.HolyShield.Reflect

                                AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity
                                    .HolyShieldEntity
                                    .SurvivalEntity ->
                                    AbilityRollPurpose.Passive.Outcome.HolyShield.Survival
                            }

                        is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.LuckyCoinEntity ->
                            when (this) {
                                AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.LuckyCoinEntity
                                    .ArmorClassEntity ->
                                    AbilityRollPurpose.Passive.Outcome.LuckyCoin.ArmorClass

                                AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.LuckyCoinEntity
                                    .ReflectEntity ->
                                    AbilityRollPurpose.Passive.Outcome.LuckyCoin.Reflect
                            }

                        is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.MirrorImagePassEntity ->
                            AbilityRollPurpose.Passive.Outcome.MirrorImagePass(
                                firstDie = firstDie,
                                secondDie = secondDie,
                                triggeredByArmorRoll = triggeredByArmorRoll,
                            )

                        is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.MirrorImageRerollEntity ->
                            AbilityRollPurpose.Passive.Outcome.MirrorImageReroll(
                                firstDie = firstDie,
                                secondDie = secondDie,
                                rerollFirstDie = rerollFirstDie,
                                triggeredByArmorRoll = triggeredByArmorRoll,
                            )

                        is AbilityRollPurposeEntity.PassiveEntity.OutcomeEntity.TeleportEntity ->
                            AbilityRollPurpose.Passive.Outcome.Teleport(defenderRoll = defenderRoll)
                    }

                is AbilityRollPurposeEntity.PassiveEntity.ReactionEntity ->
                    when (this) {
                        AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.HolyShieldEntity ->
                            AbilityRollPurpose.Passive.Reaction.HolyShield

                        AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.LuckyCoinEntity ->
                            AbilityRollPurpose.Passive.Reaction.LuckyCoin

                        is AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.MirrorImageEntity ->
                            AbilityRollPurpose.Passive.Reaction.MirrorImage(
                                firstDie = firstDie,
                                secondDie = secondDie,
                                triggeredByArmorRoll = triggeredByArmorRoll,
                            )

                        AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.PersuadeEntity ->
                            AbilityRollPurpose.Passive.Reaction.Persuade

                        AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.RiposteEntity ->
                            AbilityRollPurpose.Passive.Reaction.Riposte

                        AbilityRollPurposeEntity.PassiveEntity.ReactionEntity.TeleportEntity ->
                            AbilityRollPurpose.Passive.Reaction.Teleport
                    }
            }
    }
