package com.linklabs.sorcerersandspirits.ui.screens.arena.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.ui.theme.SorcerersAndSpiritsTheme

/**
 * Runnable demo for the avatar layer. The avatar here is drawn procedurally with [Canvas] (no
 * assets needed) purely so the motion is visible immediately — in the real game this same slot
 * would hold an `Image(painterResource(...))` sprite or a Lottie character. Tap a button to fire
 * that [AvatarGesture]; [CombatAvatar] animates whatever is inside it.
 *
 * Run it on a device/emulator to see motion (Compose `@Preview` is a static snapshot and won't show
 * the animation playing). Drop [CombatAvatarDemo] into any screen, or use the IDE preview to see
 * the layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CombatAvatarDemo(modifier: Modifier = Modifier) {
    // gestureKey changes each tap so CombatAvatar replays even the same gesture twice in a row.
    var gesture by remember { mutableStateOf(AvatarGesture.None) }
    var gestureKey by remember { mutableStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = "Avatar gesture demo", style = MaterialTheme.typography.titleLarge)

        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            CombatAvatar(gesture = gesture, gestureKey = gestureKey) {
                PlaceholderSprite(modifier = Modifier.size(120.dp))
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarGesture.entries
                .filter { it != AvatarGesture.None }
                .forEach { entry ->
                    Button(
                        onClick = {
                            gesture = entry
                            gestureKey++
                        }
                    ) {
                        Text(text = entry.name)
                    }
                }
        }
    }
}

/**
 * A stand-in "sprite" drawn with [Canvas]: a little hooded figure. This is option 4 (procedural
 * Canvas drawing) from the avatar approaches — handy as a placeholder, but in practice you'd swap
 * this for an `Image` sprite or a Lottie character.
 */
@Composable
private fun PlaceholderSprite(modifier: Modifier = Modifier) {
    val bodyColor = MaterialTheme.colorScheme.primary
    val faceColor = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Cloak / body: a rounded triangle-ish blob.
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x = w * 0.2f, y = h * 0.35f),
            size = androidx.compose.ui.geometry.Size(width = w * 0.6f, height = h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = w * 0.2f, y = w * 0.2f),
        )

        // Head.
        drawCircle(
            color = bodyColor,
            radius = w * 0.18f,
            center = Offset(x = w * 0.5f, y = h * 0.3f),
        )

        // Eyes.
        drawCircle(
            color = faceColor,
            radius = w * 0.03f,
            center = Offset(x = w * 0.43f, y = h * 0.3f),
        )
        drawCircle(
            color = faceColor,
            radius = w * 0.03f,
            center = Offset(x = w * 0.57f, y = h * 0.3f),
        )

        // A staff, to make the lunge/cast direction read.
        drawLine(
            color = Color(0xFF8D6E63),
            start = Offset(x = w * 0.78f, y = h * 0.2f),
            end = Offset(x = w * 0.78f, y = h * 0.9f),
            strokeWidth = w * 0.04f,
        )
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = w * 0.06f,
            center = Offset(x = w * 0.78f, y = h * 0.2f),
        )
    }
}

@Preview
@Composable
private fun CombatAvatarDemoPreview() {
    SorcerersAndSpiritsTheme { CombatAvatarDemo() }
}
