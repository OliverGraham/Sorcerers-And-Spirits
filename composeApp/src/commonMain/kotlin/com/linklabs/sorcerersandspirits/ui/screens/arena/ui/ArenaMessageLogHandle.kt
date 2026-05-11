package com.linklabs.sorcerersandspirits.ui.screens.arena.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.common.WeightSpacer
import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaActionsProvider
import com.linklabs.sorcerersandspirits.ui.screens.arena.model.ArenaMessageState
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme
import kotlinx.coroutines.delay

@Composable
internal fun ArenaMessageLogHandle(
    secondsRemaining: Int,
    latestMessage: ArenaMessageState?,
    unhandledMessagesCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = secondsRemaining,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(220, delayMillis = 90),
                ) togetherWith scaleOut(animationSpec = tween(220))
            },
        ) { seconds ->
            Text(text = "$seconds")
        }

        WeightSpacer()

        // TODO Arena: Improve animation
        AnimatedContent(
            targetState = latestMessage,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 1000, delayMillis = 90),
                ) togetherWith scaleOut(animationSpec = tween(durationMillis = 1000))
            },
        ) { displayedMessage ->
            if (displayedMessage != null) {
                Text(text = displayedMessage.getDisplayString())
            }

            if (unhandledMessagesCount > 0) {
                val onIncomingMessageHandled = ArenaActionsProvider.current.onIncomingMessageHandled
                LaunchedEffect(key1 = transition.currentState, key2 = transition.targetState) {
                    if (transition.currentState == transition.targetState) {
                        delay(timeMillis = 1000)
                        onIncomingMessageHandled()
                    }
                }
            }
        }
        WeightSpacer()
    }
}

@Preview(showBackground = true)
@Composable
private fun ArenaMessageLogHandlePreview() {
    SorcerersAndSpiritsTheme {
        ArenaMessageLogHandle(
            secondsRemaining = 15,
            latestMessage = ArenaMessageState.NewRound(id = "1", roundNumber = "1"),
            unhandledMessagesCount = 0,
        )
    }
}
