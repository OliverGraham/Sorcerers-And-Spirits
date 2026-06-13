package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import kotlinx.serialization.Serializable

@Serializable data class DoublesBonusEntity(val userId: String)

fun DoublesBonusEntity.toDomain() = DoublesBonus(userId = userId)

fun DoublesBonus.toEntity() = DoublesBonusEntity(userId = userId)
