package com.linklabs.sorcerersandspirits.ui.screens.arena.ui.combat.messagelog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageGroupState
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

private const val SHEET_DISMISS_FLING_VELOCITY = 400f

private enum class LogSheetAnchor { Open, Dismissed }

@Composable
internal fun MessageLogDropdown(
    messageGroups: List<ArenaMessageGroupState>,
    isExpanded: Boolean,
    barBounds: Rect?,
    diceHomeBounds: Rect?,
    onDismiss: () -> Unit,
) {
    if (barBounds == null || diceHomeBounds == null) return

    val density = LocalDensity.current
    val barHeightPx = barBounds.height
    val fullLogHeightPx = diceHomeBounds.top - barBounds.bottom
    val fullLogHeight = with(receiver = density) { fullLogHeightPx.toDp() }
    val logWidth = with(receiver = density) { barBounds.width.toDp() }
    val leftOffsetPx = barBounds.left.roundToInt()
    val topOffsetPx = barBounds.bottom.roundToInt()

    val sheetState = remember { AnchoredDraggableState(initialValue = LogSheetAnchor.Dismissed) }
    LaunchedEffect(key1 = fullLogHeightPx) {
        sheetState.updateAnchors(
            newAnchors =
            DraggableAnchors {
                LogSheetAnchor.Open at 0f
                LogSheetAnchor.Dismissed at -fullLogHeightPx
            },
        )
    }
    LaunchedEffect(key1 = isExpanded) {
        val target = if (isExpanded) LogSheetAnchor.Open else LogSheetAnchor.Dismissed
        sheetState.animateTo(targetValue = target)
    }
    LaunchedEffect(key1 = sheetState) {
        snapshotFlow { sheetState.settledValue }
            .drop(count = 1)
            .collect { settled -> if (settled == LogSheetAnchor.Dismissed) onDismiss() }
    }

    val dismissConnection =
        remember(key1 = sheetState) { logSheetDismissConnection(state = sheetState) }

    val isSheetVisible by remember(key1 = fullLogHeightPx, key2 = isExpanded) {
        derivedStateOf {
            isExpanded || (!sheetState.offset.isNaN() && sheetState.offset > -fullLogHeightPx)
        }
    }
    if (!isSheetVisible) return

    val positionProvider =
        remember(key1 = leftOffsetPx, key2 = topOffsetPx) {
            logPopupPositionProvider(leftOffsetPx = leftOffsetPx, topOffsetPx = topOffsetPx)
        }

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = Modifier.width(width = logWidth).height(height = fullLogHeight).clipToBounds(),
        ) {
            ArenaMessageLog(
                messageGroups = messageGroups,
                dismissConnection = dismissConnection,
                modifier =
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        translationY =
                            if (sheetState.offset.isNaN()) -fullLogHeightPx else sheetState.offset
                    }
                    .background(
                        brush =
                        messagesBackgroundBrush(startY = -barHeightPx, endY = fullLogHeightPx),
                    )
                    .clickable(onClick = onDismiss),
            )
        }
    }
}

private fun logPopupPositionProvider(leftOffsetPx: Int, topOffsetPx: Int) = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset(x = leftOffsetPx, y = topOffsetPx)
}

private fun logSheetDismissConnection(
    state: AnchoredDraggableState<LogSheetAnchor>,
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
        return Offset(x = 0f, y = state.dispatchRawDelta(delta = available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val isReExtendFling =
            available.y > 0f && state.requireOffset() < state.anchors.maxPosition()
        if (!isReExtendFling) return Velocity.Zero
        state.settleToNearestAnchor(velocity = available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settleToNearestAnchor(velocity = available.y)
        return available
    }
}

private suspend fun AnchoredDraggableState<LogSheetAnchor>.settleToNearestAnchor(velocity: Float) {
    val target =
        when {
            velocity <= -SHEET_DISMISS_FLING_VELOCITY -> LogSheetAnchor.Dismissed
            velocity >= SHEET_DISMISS_FLING_VELOCITY -> LogSheetAnchor.Open
            else -> anchors.closestAnchor(position = requireOffset()) ?: LogSheetAnchor.Open
        }

    animateTo(targetValue = target)
}
