package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import kotlinx.coroutines.flow.drop

private const val LOG_HEIGHT_FRACTION = 0.47f
private const val LOG_REVEAL_ANIM_LABEL = "MessageLogReveal"
private const val SHEET_DISMISS_POSITION_FRACTION = 0.6f

private enum class LogSheetAnchor { Open, Dismissed }

@Composable
internal fun ExpandingMessageLog(
    isExpanded: Boolean,
    messageGroups: List<ArenaMessageGroupState>,
    onDismiss: () -> Unit,
    isCombatOngoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val revealProgress by
        animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            label = LOG_REVEAL_ANIM_LABEL,
        )

    var panelHeight by remember { mutableStateOf(value = 0f) }
    val sheetState = remember { AnchoredDraggableState(initialValue = LogSheetAnchor.Open) }
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = panelHeight) {
        if (panelHeight <= 0f) return@LaunchedEffect
        sheetState.updateAnchors(
            newAnchors =
            DraggableAnchors {
                LogSheetAnchor.Open at 0f
                LogSheetAnchor.Dismissed at -panelHeight
            },
        )
    }
    LaunchedEffect(key1 = isExpanded) {
        if (isExpanded && panelHeight > 0f) sheetState.animateTo(targetValue = LogSheetAnchor.Open)
    }
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { sheetState.settledValue }
            .drop(count = 1)
            .collect { settled -> if (settled == LogSheetAnchor.Dismissed) onDismiss() }
    }

    if (!isExpanded && revealProgress == 0f) return

    val dismissConnection =
        remember { logSheetDismissConnection(state = sheetState, listState = listState) }

    ArenaMessageLog(
        messageGroups = messageGroups,
        listState = listState,
        dismissConnection = dismissConnection,
        isCombatOngoing = isCombatOngoing,
        modifier = modifier
            .windowInsetsPadding(insets = WindowInsets.statusBars)
            .padding(top = messageBarHeight)
            .fillMaxWidth()
            .fillMaxHeight(fraction = LOG_HEIGHT_FRACTION)
            .onSizeChanged { panelHeight = it.height.toFloat() }
            .clipToBounds()
            .graphicsLayer {
                val revealTranslation = (revealProgress - 1f) * size.height
                val dragTranslation = if (sheetState.offset.isNaN()) 0f else sheetState.offset
                translationY = minOf(revealTranslation, dragTranslation)
            }
            .background(color = messagesBackgroundColor)
            .clickable(onClick = onDismiss),
    )
}

private fun logSheetDismissConnection(
    state: AnchoredDraggableState<LogSheetAnchor>,
    listState: LazyListState,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val isReExtending = available.y > 0f && source == NestedScrollSource.UserInput
        if (!isReExtending) return Offset.Zero
        return Offset(x = 0f, y = state.dispatchRawDelta(delta = available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val isDismissDrag = available.y < 0f
        if (isDismissDrag && listState.canScrollForward) return Offset.Zero
        return Offset(x = 0f, y = state.dispatchRawDelta(delta = available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val isReExtendFling =
            available.y > 0f && state.requireOffset() < state.anchors.maxPosition()
        if (!isReExtendFling) return Velocity.Zero
        state.settleToNearestAnchor()
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settleToNearestAnchor()
        return available
    }
}

private suspend fun AnchoredDraggableState<LogSheetAnchor>.settleToNearestAnchor() {
    val dismissThreshold = anchors.minPosition() * SHEET_DISMISS_POSITION_FRACTION
    val hasDraggedPastThreshold = requireOffset() <= dismissThreshold
    val target = if (hasDraggedPastThreshold) LogSheetAnchor.Dismissed else LogSheetAnchor.Open

    animateTo(targetValue = target)
}
