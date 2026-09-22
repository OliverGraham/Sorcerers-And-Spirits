package com.linklabs.sorcerersandspirits.ui.main.navigation.transitions

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val SHATTER_COLUMNS = 8
private const val SHATTER_ROWS = 14
private const val SHATTER_MIN_SPEED = 260f
private const val SHATTER_MAX_SPEED = 620f
private const val SHATTER_UPWARD_BIAS = 180f
private const val SHATTER_GRAVITY_PIXELS = 900f
private const val SHATTER_MAX_ANGULAR_DEGREES = 240f
private const val SHATTER_HOLD_FRACTION = 0.12f

@Composable
fun ShatterDissolve(snapshot: ImageBitmap, progress: Float, modifier: Modifier = Modifier) {
    val shards = remember(key1 = snapshot) { snapshot.toShards() }

    Canvas(modifier = modifier) {
        val flightProgress =
            ((progress - SHATTER_HOLD_FRACTION) / (1f - SHATTER_HOLD_FRACTION)).coerceIn(
                minimumValue = 0f,
                maximumValue = 1f,
            )
        val life = 1f - flightProgress
        shards.forEach { shard ->
            val gravityFall = SHATTER_GRAVITY_PIXELS * flightProgress * flightProgress
            val dx = shard.velocityX * flightProgress
            val dy = shard.velocityY * flightProgress + gravityFall
            val angle = shard.angularVelocity * flightProgress
            val pivot = Offset(x = shard.centerX + dx, y = shard.centerY + dy)
            rotate(degrees = angle, pivot = pivot) {
                drawImage(
                    image = snapshot,
                    srcOffset = IntOffset(x = shard.x, y = shard.y),
                    srcSize = IntSize(width = shard.width, height = shard.height),
                    dstOffset =
                    IntOffset(x = (shard.x + dx).roundToInt(), y = (shard.y + dy).roundToInt()),
                    dstSize = IntSize(width = shard.width, height = shard.height),
                    alpha = life,
                )
            }
        }
    }
}

private fun ImageBitmap.toShards(): List<Shard> {
    val tileWidth = (width / SHATTER_COLUMNS).coerceAtLeast(minimumValue = 1)
    val tileHeight = (height / SHATTER_ROWS).coerceAtLeast(minimumValue = 1)
    val screenCenter = Offset(x = width / 2f, y = height / 2f)
    val shards = mutableListOf<Shard>()

    var row = 0
    while (row < SHATTER_ROWS) {
        var column = 0
        while (column < SHATTER_COLUMNS) {
            val tileX = column * tileWidth
            val tileY = row * tileHeight
            val isLastColumn = column == SHATTER_COLUMNS - 1
            val isLastRow = row == SHATTER_ROWS - 1
            val actualWidth = if (isLastColumn) width - tileX else tileWidth
            val actualHeight = if (isLastRow) height - tileY else tileHeight
            val centerX = tileX + actualWidth / 2f
            val centerY = tileY + actualHeight / 2f
            val awayX = centerX - screenCenter.x
            val awayY = centerY - screenCenter.y
            val distance = sqrt(awayX * awayX + awayY * awayY).coerceAtLeast(minimumValue = 1f)
            val speed = randomBetween(min = SHATTER_MIN_SPEED, max = SHATTER_MAX_SPEED)
            shards.add(
                Shard(
                    x = tileX,
                    y = tileY,
                    width = actualWidth,
                    height = actualHeight,
                    centerX = centerX,
                    centerY = centerY,
                    velocityX = awayX / distance * speed,
                    velocityY = awayY / distance * speed -
                        randomBetween(min = 0f, max = SHATTER_UPWARD_BIAS),
                    angularVelocity =
                    randomBetween(
                        min = -SHATTER_MAX_ANGULAR_DEGREES,
                        max = SHATTER_MAX_ANGULAR_DEGREES,
                    ),
                ),
            )
            column++
        }
        row++
    }

    return shards
}

private class Shard(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val centerX: Float,
    val centerY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val angularVelocity: Float,
)
