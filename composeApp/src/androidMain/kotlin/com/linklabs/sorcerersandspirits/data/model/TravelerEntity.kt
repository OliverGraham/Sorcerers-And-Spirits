package com.linklabs.sorcerersandspirits.data.model

import com.linklabs.sorcerersandspirits.domain.traveler.InviteStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler
import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus
import kotlinx.serialization.Serializable

// TODO: Update Spotless
/**
 * Have to flatten the properties for [TravelerEntity], since Firebase cannot make sense of a sealed
 * interface. [TravelerStatusType], [senderOfArenaChallengeInviteIsThisTravelerEntity] and
 * [arenaChallengeInviteStatus] are the solution. [senderOfArenaChallengeInviteIsThisTravelerEntity]
 * is named as such because Firebase uses Java Bean, and drops the "is" prefix for boolean
 * properties.
 */
@Serializable
internal data class TravelerEntity(
    val id: String = "",
    val displayName: String = "",
    val statusType: TravelerStatusType = TravelerStatusType.Available,
    val senderOfArenaChallengeInviteIsThisTravelerEntity: Boolean? = null,
    val arenaChallengeInviteStatus: InviteStatus? = null,
) {
    fun toTraveler(): Traveler? {
        if (id.isEmpty() || displayName.isEmpty()) return null

        val travelerStatus =
            when (statusType) {
                TravelerStatusType.Available -> TravelerStatus.Available
                TravelerStatusType.InArena -> TravelerStatus.InArena
                TravelerStatusType.Offline -> TravelerStatus.Offline
                TravelerStatusType.InvitedToArena ->
                    TravelerStatus.InvitedToArena(
                        isSenderOfInvite =
                            senderOfArenaChallengeInviteIsThisTravelerEntity ?: false,
                        inviteStatus = arenaChallengeInviteStatus ?: InviteStatus.None,
                    )
            }

        return Traveler(id = id, displayName = displayName, status = travelerStatus)
    }

    companion object {
        fun getIdPropertyName() = TravelerEntity::id.name
    }
}

internal enum class TravelerStatusType {
    Available,
    InArena,
    Offline,
    InvitedToArena,
}

internal fun Traveler.toTravelerEntity(): TravelerEntity {
    val statusType = status.toTravelerStatusType()

    val (isSenderOfInvite, inviteStatus) =
        if (status is TravelerStatus.InvitedToArena) {
            Pair(first = status.isSenderOfInvite, second = status.inviteStatus)
        } else {
            Pair(first = null, second = null)
        }

    return TravelerEntity(
        id = id,
        displayName = displayName,
        statusType = statusType,
        senderOfArenaChallengeInviteIsThisTravelerEntity = isSenderOfInvite,
        arenaChallengeInviteStatus = inviteStatus,
    )
}

private fun TravelerStatus.toTravelerStatusType() =
    when (this) {
        is TravelerStatus.Available -> TravelerStatusType.Available
        is TravelerStatus.InArena -> TravelerStatusType.InArena
        is TravelerStatus.Offline -> TravelerStatusType.Offline
        is TravelerStatus.InvitedToArena -> TravelerStatusType.InvitedToArena
    }
