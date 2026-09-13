package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.animation.necromancer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.MinionState
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import org.jetbrains.compose.resources.imageResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.golem_minion
import sorcerersandspirits.composeapp.generated.resources.skeleton_minion

@Composable
internal fun SummonedMinions(
    minions: List<MinionState>,
    attackingMinionIndex: Int?,
    animatingMessage: ArenaMessageState?,
    modifier: Modifier = Modifier,
) {
    val skeletonImage = imageResource(resource = Res.drawable.skeleton_minion)
    val golemImage = imageResource(resource = Res.drawable.golem_minion)

    val explodedMinionNames = remember { mutableStateSetOf<String>() }

    LaunchedEffect(key1 = animatingMessage) {
        if (animatingMessage is ArenaMessageState.SummonMinion.MinionDestroyed) {
            explodedMinionNames.add(element = animatingMessage.minionName)
        }
    }

    LaunchedEffect(key1 = minions) {
        explodedMinionNames.retainAll(elements = minions.map { it.name }.toSet())
    }

    Canvas(modifier = modifier) {
        drawGatheredMinions(
            minions = minions,
            attackingMinionIndex = attackingMinionIndex,
            explodedMinionNames = explodedMinionNames,
            skeletonImage = skeletonImage,
            golemImage = golemImage,
        )
    }
}

private fun DrawScope.drawGatheredMinions(
    minions: List<MinionState>,
    attackingMinionIndex: Int?,
    explodedMinionNames: Set<String>,
    skeletonImage: ImageBitmap,
    golemImage: ImageBitmap,
) {
    val area = Rect(offset = Offset.Zero, size = size)

    minions.indices.reversed().forEach { index ->
        if (index == attackingMinionIndex) return@forEach

        val minion = minions[index]
        if (minion.name in explodedMinionNames) return@forEach

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
