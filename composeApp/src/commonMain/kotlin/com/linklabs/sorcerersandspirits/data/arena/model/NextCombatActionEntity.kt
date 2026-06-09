package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import kotlinx.serialization.Serializable

@Serializable
sealed interface NextCombatActionEntity {
    val activeUserIds: Set<String>

    @Serializable
    data class IdleEntity(override val activeUserIds: Set<String>) : NextCombatActionEntity

    @Serializable
    data class NormalAttackEntity(
        val roll: NormalAttackRollEntity,
        override val activeUserIds: Set<String>,
    ) : NextCombatActionEntity

    @Serializable
    data class AbilityRollEntity(
        val purpose: AbilityActionEntity,
        override val activeUserIds: Set<String>,
    ) : NextCombatActionEntity
}

@Serializable
sealed interface NormalAttackRollEntity {
    @Serializable
    data class ArmorClassEntity(
        val attackerUserId: String,
        val defenderUserId: String,
        val originalTurnUserId: String,
    ) : NormalAttackRollEntity

    @Serializable
    data class HealthEntity(val attackerUserId: String, val defenderUserId: String) :
        NormalAttackRollEntity
}

@Serializable
sealed interface AbilityActionEntity {

    @Serializable
    sealed interface ActivatedEntity : AbilityActionEntity {
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
    sealed interface PassiveEntity : AbilityActionEntity {

        @Serializable
        sealed interface LuckyCoinEntity : PassiveEntity {
            @Serializable data class FlipEntity(val attackerRollTotal: Int) : LuckyCoinEntity

            @Serializable
            data class HighRollerEntity(val attackerUserId: String, val attackerRollTotal: Int) :
                LuckyCoinEntity
        }

        @Serializable data class RiposteEntity(val attackerRollTotal: Int) : PassiveEntity

        @Serializable
        sealed interface PersuadeEntity : PassiveEntity {
            @Serializable data object StrumLuteEntity : PersuadeEntity

            @Serializable
            data class CharmEntity(
                val attackerUserId: String,
                val charmTargetUserIds: Set<String>,
                val selectedCharmTargetUserId: String? = null,
            ) : PersuadeEntity
        }

        @Serializable data object EncoreEntity : PassiveEntity

        @Serializable
        data class TeleportEntity(val attackerRoll: Int? = null, val defenderRoll: Int? = null) :
            PassiveEntity

        @Serializable
        data class MirrorImageEntity(
            val firstDie: MirrorImageDieEntity,
            val secondDie: MirrorImageDieEntity,
            val attackType: NormalAttackTypeEntity,
            val mirrorImageDefensiveAbility: DefensiveAbilityEntity.MirrorImage,
        ) : PassiveEntity

        @Serializable
        data class MirrorImageDieEntity(val originalRoll: Int, val reroll: Int? = null)

        @Serializable
        sealed interface HolyShieldEntity : PassiveEntity {
            @Serializable data object PrayerEntity : HolyShieldEntity

            @Serializable data object WrathEntity : HolyShieldEntity
        }
    }
}

@Serializable
enum class NormalAttackTypeEntity {
    ArmorClass,
    Health,
}

internal fun NextCombatAction.toEntity(): NextCombatActionEntity =
    when (this) {
        is NextCombatAction.Idle -> NextCombatActionEntity.IdleEntity(activeUserIds = activeUserIds)

        is NextCombatAction.NormalAttack ->
            NextCombatActionEntity.NormalAttackEntity(
                roll = roll.toEntity(),
                activeUserIds = activeUserIds,
            )

        is NextCombatAction.AbilityRoll ->
            NextCombatActionEntity.AbilityRollEntity(
                purpose = purpose.toEntity(),
                activeUserIds = activeUserIds,
            )
    }

internal fun NormalAttackRoll.toEntity(): NormalAttackRollEntity =
    when (this) {
        is NormalAttackRoll.ArmorClass ->
            NormalAttackRollEntity.ArmorClassEntity(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
            )

        is NormalAttackRoll.Health ->
            NormalAttackRollEntity.HealthEntity(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )
    }

internal fun NextCombatActionEntity.toDomain(): NextCombatAction =
    when (this) {
        is NextCombatActionEntity.IdleEntity -> NextCombatAction.Idle(activeUserIds = activeUserIds)

        is NextCombatActionEntity.NormalAttackEntity ->
            NextCombatAction.NormalAttack(roll = roll.toDomain(), activeUserIds = activeUserIds)

        is NextCombatActionEntity.AbilityRollEntity ->
            NextCombatAction.AbilityRoll(
                purpose = purpose.toDomain(),
                activeUserIds = activeUserIds,
            )
    }

internal fun NormalAttackRollEntity.toDomain(): NormalAttackRoll =
    when (this) {
        is NormalAttackRollEntity.ArmorClassEntity ->
            NormalAttackRoll.ArmorClass(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                originalTurnUserId = originalTurnUserId,
            )

        is NormalAttackRollEntity.HealthEntity ->
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )
    }

internal fun AbilityAction.toEntity(): AbilityActionEntity =
    when (this) {
        is AbilityAction.Activated ->
            when (this) {
                is AbilityAction.Activated.AllIn ->
                    AbilityActionEntity.ActivatedEntity.AllInEntity(
                        allInAbility = allInAbility.toEntity() as OffensiveAbilityEntity.AllIn
                    )

                is AbilityAction.Activated.Impersonate ->
                    AbilityActionEntity.ActivatedEntity.ImpersonateEntity(
                        impersonateAbility =
                            impersonateAbility.toEntity() as OffensiveAbilityEntity.Impersonate
                    )

                AbilityAction.Activated.PrepareAttack ->
                    AbilityActionEntity.ActivatedEntity.PrepareAttackEntity

                is AbilityAction.Activated.SummonMinion ->
                    AbilityActionEntity.ActivatedEntity.SummonMinionEntity(
                        summonMinionAbility =
                            summonMinionAbility.toEntity() as OffensiveAbilityEntity.SummonMinion
                    )
            }

        is AbilityAction.Passive ->
            when (this) {
                is AbilityAction.Passive.LuckyCoin ->
                    when (this) {
                        is AbilityAction.Passive.LuckyCoin.Flip ->
                            AbilityActionEntity.PassiveEntity.LuckyCoinEntity.FlipEntity(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityAction.Passive.LuckyCoin.HighRoller ->
                            AbilityActionEntity.PassiveEntity.LuckyCoinEntity.HighRollerEntity(
                                attackerUserId = attackerUserId,
                                attackerRollTotal = attackerRollTotal,
                            )
                    }

                is AbilityAction.Passive.Riposte ->
                    AbilityActionEntity.PassiveEntity.RiposteEntity(
                        attackerRollTotal = attackerRollTotal
                    )

                is AbilityAction.Passive.Persuade ->
                    when (this) {
                        is AbilityAction.Passive.Persuade.StrumLute ->
                            AbilityActionEntity.PassiveEntity.PersuadeEntity.StrumLuteEntity

                        is AbilityAction.Passive.Persuade.Charm ->
                            AbilityActionEntity.PassiveEntity.PersuadeEntity.CharmEntity(
                                attackerUserId = attackerUserId,
                                charmTargetUserIds = charmTargetUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityAction.Passive.Encore -> AbilityActionEntity.PassiveEntity.EncoreEntity

                is AbilityAction.Passive.Teleport ->
                    AbilityActionEntity.PassiveEntity.TeleportEntity(
                        attackerRoll = attackerRoll,
                        defenderRoll = defenderRoll,
                    )

                is AbilityAction.Passive.MirrorImage ->
                    AbilityActionEntity.PassiveEntity.MirrorImageEntity(
                        firstDie =
                            AbilityActionEntity.PassiveEntity.MirrorImageDieEntity(
                                originalRoll = firstDie.originalRoll,
                                reroll = firstDie.reroll,
                            ),
                        secondDie =
                            AbilityActionEntity.PassiveEntity.MirrorImageDieEntity(
                                originalRoll = secondDie.originalRoll,
                                reroll = secondDie.reroll,
                            ),
                        attackType =
                            when (attackType) {
                                com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
                                    .ArmorClass -> NormalAttackTypeEntity.ArmorClass

                                com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
                                    .Health -> NormalAttackTypeEntity.Health
                            },
                        mirrorImageDefensiveAbility =
                            mirrorImageDefensiveAbility.toEntity()
                                as DefensiveAbilityEntity.MirrorImage,
                    )

                is AbilityAction.Passive.HolyShield ->
                    when (this) {
                        AbilityAction.Passive.HolyShield.Prayer ->
                            AbilityActionEntity.PassiveEntity.HolyShieldEntity.PrayerEntity

                        AbilityAction.Passive.HolyShield.Wrath ->
                            AbilityActionEntity.PassiveEntity.HolyShieldEntity.WrathEntity
                    }
            }
    }

internal fun AbilityActionEntity.toDomain(): AbilityAction =
    when (this) {
        is AbilityActionEntity.ActivatedEntity ->
            when (this) {
                is AbilityActionEntity.ActivatedEntity.AllInEntity ->
                    AbilityAction.Activated.AllIn(
                        allInAbility = allInAbility.toDomain() as OffensiveAbility.AllIn
                    )

                is AbilityActionEntity.ActivatedEntity.ImpersonateEntity ->
                    AbilityAction.Activated.Impersonate(
                        impersonateAbility =
                            impersonateAbility.toDomain() as OffensiveAbility.Impersonate
                    )

                AbilityActionEntity.ActivatedEntity.PrepareAttackEntity ->
                    AbilityAction.Activated.PrepareAttack

                is AbilityActionEntity.ActivatedEntity.SummonMinionEntity ->
                    AbilityAction.Activated.SummonMinion(
                        summonMinionAbility =
                            summonMinionAbility.toDomain() as OffensiveAbility.SummonMinion
                    )
            }

        is AbilityActionEntity.PassiveEntity ->
            when (this) {
                is AbilityActionEntity.PassiveEntity.LuckyCoinEntity ->
                    when (this) {
                        is AbilityActionEntity.PassiveEntity.LuckyCoinEntity.FlipEntity ->
                            AbilityAction.Passive.LuckyCoin.Flip(
                                attackerRollTotal = attackerRollTotal
                            )

                        is AbilityActionEntity.PassiveEntity.LuckyCoinEntity.HighRollerEntity ->
                            AbilityAction.Passive.LuckyCoin.HighRoller(
                                attackerUserId = attackerUserId,
                                attackerRollTotal = attackerRollTotal,
                            )
                    }

                is AbilityActionEntity.PassiveEntity.RiposteEntity ->
                    AbilityAction.Passive.Riposte(attackerRollTotal = attackerRollTotal)

                is AbilityActionEntity.PassiveEntity.PersuadeEntity ->
                    when (this) {
                        is AbilityActionEntity.PassiveEntity.PersuadeEntity.StrumLuteEntity ->
                            AbilityAction.Passive.Persuade.StrumLute

                        is AbilityActionEntity.PassiveEntity.PersuadeEntity.CharmEntity ->
                            AbilityAction.Passive.Persuade.Charm(
                                attackerUserId = attackerUserId,
                                charmTargetUserIds = charmTargetUserIds,
                                selectedCharmTargetUserId = selectedCharmTargetUserId,
                            )
                    }

                AbilityActionEntity.PassiveEntity.EncoreEntity -> AbilityAction.Passive.Encore

                is AbilityActionEntity.PassiveEntity.TeleportEntity ->
                    AbilityAction.Passive.Teleport(
                        attackerRoll = attackerRoll,
                        defenderRoll = defenderRoll,
                    )

                is AbilityActionEntity.PassiveEntity.MirrorImageEntity ->
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
                        attackType =
                            when (attackType) {
                                NormalAttackTypeEntity.ArmorClass ->
                                    com.linklabs.sorcerersandspirits.domain.arena.model
                                        .NormalAttackType
                                        .ArmorClass

                                NormalAttackTypeEntity.Health ->
                                    com.linklabs.sorcerersandspirits.domain.arena.model
                                        .NormalAttackType
                                        .Health
                            },
                        mirrorImageDefensiveAbility =
                            mirrorImageDefensiveAbility.toDomain() as DefensiveAbility.MirrorImage,
                    )

                is AbilityActionEntity.PassiveEntity.HolyShieldEntity ->
                    when (this) {
                        AbilityActionEntity.PassiveEntity.HolyShieldEntity.PrayerEntity ->
                            AbilityAction.Passive.HolyShield.Prayer

                        AbilityActionEntity.PassiveEntity.HolyShieldEntity.WrathEntity ->
                            AbilityAction.Passive.HolyShield.Wrath
                    }
            }
    }
