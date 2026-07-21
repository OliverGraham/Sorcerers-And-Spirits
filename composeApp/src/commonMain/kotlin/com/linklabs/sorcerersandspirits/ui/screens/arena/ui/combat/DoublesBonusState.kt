package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus

@Immutable data class DoublesBonusState(val userId: String)

internal fun DoublesBonus.toState() = DoublesBonusState(userId = userId)

internal fun DoublesBonusState.toDomain() = DoublesBonus(userId = userId)
