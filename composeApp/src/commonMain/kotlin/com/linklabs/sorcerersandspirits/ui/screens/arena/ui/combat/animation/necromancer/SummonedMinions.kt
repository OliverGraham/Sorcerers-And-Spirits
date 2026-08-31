package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.MinionState
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion

@Composable
internal fun SummonedMinions(
    minions: List<MinionState>,
    summonerUserId: String,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val skeletonImage = imageResource(resource = Res.drawable.skeleton_minion)
    val golemImage = imageResource(resource = Res.drawable.golem_minion)

    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            reportMinionBounds(
                rootBounds = coordinates.boundsInRoot(),
                minions = minions,
                summonerUserId = summonerUserId,
                onAvatarBoundsChanged = onAvatarBoundsChanged,
            )
        },
    ) {
        drawGatheredMinions(
            minions = minions,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private fun reportMinionBounds(
    rootBounds: Rect,
    minions: List<MinionState>,
    summonerUserId: String,
    onAvatarBoundsChanged: (String, Rect) -> Unit,
) {
    val area = Rect(offset = Offset.Zero, size = rootBounds.size)

    minions.forEachIndexed { index, minion ->
        val slot = minionRestingSlot(displayIndex = index, minionType = minion.type, area = area)
        onAvatarBoundsChanged(
            minionAvatarId(summonerUserId = summonerUserId, index = index),
            slot.toRect().translate(offset = rootBounds.topLeft),
        )
    }
}

private fun DrawScope.drawGatheredMinions(
    minions: List<MinionState>,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val area = Rect(offset = Offset.Zero, size = size)

    minions.indices.reversed().forEach { index ->
        val minion = minions[index]
        val slot = minionRestingSlot(displayIndex = index, minionType = minion.type, area = area)

        drawMinion(
            kind = minion.type,
            footCenter = slot.footCenter,
            height = slot.height,
            width = slot.height,
            alpha = 1f,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}
