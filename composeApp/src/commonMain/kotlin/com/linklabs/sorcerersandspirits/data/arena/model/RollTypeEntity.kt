package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import kotlinx.serialization.Serializable

@Serializable
sealed interface RollTypeEntity {
    @Serializable data class AbilityRollEntity(val ability: AbilityEntity) : RollTypeEntity

    @Serializable data object ArmorClassRollEntity : RollTypeEntity

    @Serializable data object HealthRollEntity : RollTypeEntity
}

internal fun RollType.toEntity(): RollTypeEntity =
    when (this) {
        is RollType.AbilityRoll -> RollTypeEntity.AbilityRollEntity(ability = ability.toEntity())
        is RollType.ArmorClassRoll -> RollTypeEntity.ArmorClassRollEntity
        is RollType.HealthRoll -> RollTypeEntity.HealthRollEntity
    }

internal fun RollTypeEntity.toDomain(): RollType =
    when (this) {
        is RollTypeEntity.AbilityRollEntity -> RollType.AbilityRoll(ability = ability.toDomain())
        is RollTypeEntity.ArmorClassRollEntity -> RollType.ArmorClassRoll
        is RollTypeEntity.HealthRollEntity -> RollType.HealthRoll
    }
