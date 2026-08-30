package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.linklabs.sorcerersandspirits.domain.game.Minion
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion

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
            minions = minions.takeLast(n = MAX_RENDERED_MINIONS),
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private fun DrawScope.drawGatheredMinions(
    minions: List<Minion>,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val area = Rect(offset = Offset.Zero, size = size)

    minions.indices.reversed().forEach { index ->
        val minion = minions[index]
        val slot = minionRestingSlot(displayIndex = index, kind = minion, area = area)

        drawMinion(
            kind = minion,
            footCenter = slot.footCenter,
            height = slot.height,
            width = slot.height,
            alpha = 1f,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}
