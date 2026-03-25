package com.linklabs.sorcerersandspirits.domain.traveler

data class Traveler(val id: String, val displayName: String, val status: TravelerStatus)

sealed interface TravelerStatus {
    data object Available : TravelerStatus

    data object InArena : TravelerStatus

    data object Offline : TravelerStatus

    data class InvitedToArena(val isSenderOfInvite: Boolean, val inviteStatus: InviteStatus) :
        TravelerStatus
}

enum class InviteStatus {
    Accepted,
    Considering,
    Declined,
    None,
}
