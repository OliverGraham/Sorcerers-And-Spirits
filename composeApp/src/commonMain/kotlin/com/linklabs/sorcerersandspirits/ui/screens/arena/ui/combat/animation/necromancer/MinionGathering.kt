package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.linklabs.sorcerersandspirits.domain.game.Minion

const val MAX_RENDERED_MINIONS = 10

private const val SKELETON_HEIGHT_FRACTION = 0.42f
private const val GOLEM_HEIGHT_FRACTION = 0.5f
private const val BASE_SIDE_GAP_FRACTION = 0.2f
private const val PER_TIER_SIDE_GAP_FRACTION = 0.13f
private const val PER_TIER_RECEDE_FRACTION = 0.04f
private const val PER_TIER_SHRINK = 0.06f
private const val MIN_TIER_SCALE = 0.7f
private const val FOOT_LINE_FRACTION = 0.97f

@Immutable
internal data class MinionSlot(val footCenter: Offset, val height: Float)

internal fun minionRestingSlot(displayIndex: Int, kind: Minion, area: Rect): MinionSlot {
    val side = if (displayIndex % 2 == 0) -1f else 1f
    val tier = displayIndex / 2

    val tierScale = (1f - tier * PER_TIER_SHRINK).coerceAtLeast(minimumValue = MIN_TIER_SCALE)
    val heightFraction =
        when (kind) {
            Minion.Skeleton -> SKELETON_HEIGHT_FRACTION
            Minion.Golem -> GOLEM_HEIGHT_FRACTION
        }
    val height = area.height * heightFraction * tierScale

    val sideGap = area.width * (BASE_SIDE_GAP_FRACTION + tier * PER_TIER_SIDE_GAP_FRACTION)
    val footX = area.center.x + side * sideGap
    val footY = area.top + area.height * FOOT_LINE_FRACTION - tier * area.height * PER_TIER_RECEDE_FRACTION

    return MinionSlot(footCenter = Offset(x = footX, y = footY), height = height)
}
