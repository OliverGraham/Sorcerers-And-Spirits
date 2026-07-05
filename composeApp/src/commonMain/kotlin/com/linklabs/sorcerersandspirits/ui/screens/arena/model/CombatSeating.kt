package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

/**
 * The complete avatar seating arrangement for one combat moment: who occupies the two large-avatar
 * seats and who collapses to the head-icon column (SS-143). Derived once per state from
 * [seatingFor] and passed to the stage as a single snapshot, so every part of the layout — seats,
 * head icons, and the transition between arrangements — reads the same answer. Holds only player
 * ids: seat identity is what the transition is keyed on, while the live player data (hit points,
 * shields) is looked up at render time so it updates without re-seating anyone (SS-181).
 */
@Immutable
data class CombatSeating(
    val topSeatUserId: String?,
    val bottomSeatUserId: String?,
    val headIconUserIds: List<String>,
)

/**
 * Seats the combatants for the in-flight action: the attacker plus a distinct conscious target,
 * with everyone else collapsed to head icons. Seats are assigned by identity, not by role — the
 * user's own player always takes the bottom seat, and two seated opponents keep a fixed order — so
 * the dice changing hands (Mirror Image, Riposte, a turn passing between the same two players)
 * never rearranges the stage; only the active-roller halo moves (SS-168, SS-181).
 *
 * A [collapsingTargetUserId] holds the just-downed player in the second seat while their collapse
 * plays out, even though they are no longer conscious and the live target resolution has moved on,
 * so the Collapse animates in place instead of the pane swapping out from under it (SS-213).
 */
internal fun ArenaState.Combat.seatingFor(
    targetUserId: String,
    isMatchEnding: Boolean,
    collapsingTargetUserId: String?,
): CombatSeating {
    val forcedSecondSeatUserId =
        collapsingTargetUserId?.takeIf { it in playerMap.keys && it != attackingUserId }
    val secondSeatUserId =
        forcedSecondSeatUserId
            ?: seatedTargetUserId(targetUserId = targetUserId, isMatchEnding = isMatchEnding)
    val seatedUserIds = setOfNotNull(attackingUserId, secondSeatUserId)
    val (topSeatUserId, bottomSeatUserId) = assignSeats(seatedUserIds = seatedUserIds)

    return CombatSeating(
        topSeatUserId = topSeatUserId,
        bottomSeatUserId = bottomSeatUserId,
        headIconUserIds = playerMap.keys.filter { it !in seatedUserIds },
    )
}

/** The player occupying [seatUserId], or null for an empty seat. */
internal fun ArenaState.Combat.seatedPlayer(seatUserId: String?): PlayerState.Selected? {
    if (seatUserId == null) return null
    return playerMap[seatUserId]
}

/**
 * The second seated combatant: the conscious target, or null when there is none, it is knocked out
 * — a KO'd player drops back to a head icon instead (SS-152) — or it is the attacker itself. A
 * self-targeted ability (e.g. Summon Minion) names the actor as its own defender, and the actor is
 * already seated, so it must not take both seats (SS-168).
 *
 * The match-ending knockout is the exception: while the finish hold shows the closing KO
 * ([isMatchEnding]), the downed target keeps its seat as a large avatar rather than collapsing to a
 * head icon, since there is no further combat to promote another opponent into the seat (SS-184).
 */
private fun ArenaState.Combat.seatedTargetUserId(
    targetUserId: String,
    isMatchEnding: Boolean,
): String? {
    val target = playerMap[targetUserId] ?: return null
    val isKnockedOutMidMatch = !target.characterClass.isConscious && !isMatchEnding
    if (isKnockedOutMidMatch) return null
    return if (target.userId == attackingUserId) null else target.userId
}

/**
 * Assigns the seated players to the (top, bottom) seats by identity. The user's own player always
 * sits at the bottom, nearest their controls; a lone seated opponent takes the top seat; two seated
 * opponents are ordered by user id so the same pair always sits the same way.
 */
private fun ArenaState.Combat.assignSeats(seatedUserIds: Set<String>): Pair<String?, String?> {
    if (userPlayerId in seatedUserIds) {
        val seatedOpponentUserId = seatedUserIds.singleOrNull { it != userPlayerId }
        return seatedOpponentUserId to userPlayerId
    }

    val orderedUserIds = seatedUserIds.sorted()
    return orderedUserIds.firstOrNull() to orderedUserIds.getOrNull(index = 1)
}
