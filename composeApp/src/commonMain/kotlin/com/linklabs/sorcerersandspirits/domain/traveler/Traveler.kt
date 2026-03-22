package com.linklabs.sorcerersandspirits.domain.traveler

data class Traveler(
    val id: String,
    val displayName: String,
    val gamePlayStatus: GamePlayStatus,
)

enum class GamePlayStatus {
    Available,
    AcceptedInvite,
    ConsideringInvite,
    DeclinedInvite,
    InArena,
}
