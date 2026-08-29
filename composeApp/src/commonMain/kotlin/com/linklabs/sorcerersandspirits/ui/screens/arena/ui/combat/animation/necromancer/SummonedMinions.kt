package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.linklabs.sorcerersandspirits.domain.game.Minion
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion

private const val SKELETON_HEIGHT_FRACTION = 0.42f
private const val GOLEM_HEIGHT_FRACTION = 0.5f

private const val BASE_SIDE_GAP_FRACTION = 0.2f
private const val PER_TIER_SIDE_GAP_FRACTION = 0.13f
private const val PER_TIER_RECEDE_FRACTION = 0.04f
private const val PER_TIER_SHRINK = 0.06f
private const val MIN_TIER_SCALE = 0.7f
private const val FOOT_LINE_FRACTION = 0.97f

@Composable
internal fun SummonedMinions(
    minions: List<Minion>,
    isTopSeat: Boolean,
    modifier: Modifier = Modifier,
) {
    if (minions.isEmpty()) return

    val skeletonImage = imageResource(resource = Res.drawable.skeleton_minion)
    val golemImage = imageResource(resource = Res.drawable.golem_minion)

    Canvas(modifier = modifier) {
        drawGatheredMinions(
            minions = minions,
            isTopSeat = isTopSeat,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private fun DrawScope.drawGatheredMinions(
    minions: List<Minion>,
    isTopSeat: Boolean,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val centerX = size.width / 2f
    val footLineY = size.height * FOOT_LINE_FRACTION

    minions.indices.reversed().forEach { index ->
        val minion = minions[index]
        val side = if (index % 2 == 0) -1f else 1f
        val tier = index / 2

        val tierScale = (1f - tier * PER_TIER_SHRINK).coerceAtLeast(minimumValue = MIN_TIER_SCALE)
        val heightFraction =
            when (minion) {
                Minion.Skeleton -> SKELETON_HEIGHT_FRACTION
                Minion.Golem -> GOLEM_HEIGHT_FRACTION
            }
        val minionHeight = size.height * heightFraction * tierScale

        val sideGap = size.width * (BASE_SIDE_GAP_FRACTION + tier * PER_TIER_SIDE_GAP_FRACTION)
        val footX = centerX + side * sideGap
        val footY = footLineY - tier * size.height * PER_TIER_RECEDE_FRACTION

        drawMinion(
            kind = minion,
            footCenter = Offset(x = footX, y = footY),
            height = minionHeight,
            width = minionHeight,
            alpha = 1f,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}
