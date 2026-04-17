package com.linklabs.sorcerersandspirits.data.arena.model

import kotlinx.serialization.Serializable

// TODO Arena: Fix

@Serializable
data class ArenaMessageLogEntryEntity(
    val type: String = "", // Matches the sealed interface types in ViewModel
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

// internal fun ArenaMessageLogEntryEntity.toDomain() = null
