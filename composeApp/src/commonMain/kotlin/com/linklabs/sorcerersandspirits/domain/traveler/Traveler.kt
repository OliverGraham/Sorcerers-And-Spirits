package com.linklabs.sorcerersandspirits.domain.traveler

data class Traveler(val id: String, val displayName: String, val status: TravelerStatus) {
    val hasAcceptedInviteToArena =
        (status as? TravelerStatus.InvitedToArena)?.inviteStatus == InviteStatus.Accepted
    val hasDeclinedInviteToArena =
        (status as? TravelerStatus.InvitedToArena)?.inviteStatus == InviteStatus.Declined
}

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
