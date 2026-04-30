package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPhase
import kotlinx.serialization.Serializable

@Serializable
enum class ArenaPhaseEntity {
    WaitingForAllPlayers,
    ChoosingClasses,
    Combat,
    Finished,
}

internal fun ArenaPhaseEntity.toDomain() =
    when (this) {
        ArenaPhaseEntity.WaitingForAllPlayers -> ArenaPhase.WaitingForAllPlayers
        ArenaPhaseEntity.ChoosingClasses -> ArenaPhase.ChoosingClasses
        ArenaPhaseEntity.Combat -> ArenaPhase.Combat
        ArenaPhaseEntity.Finished -> ArenaPhase.Finished
    }

internal fun ArenaPhase.toEntity() =
    when (this) {
        ArenaPhase.WaitingForAllPlayers -> ArenaPhaseEntity.WaitingForAllPlayers
        ArenaPhase.ChoosingClasses -> ArenaPhaseEntity.ChoosingClasses
        ArenaPhase.Combat -> ArenaPhaseEntity.Combat
        ArenaPhase.Finished -> ArenaPhaseEntity.Finished
    }
