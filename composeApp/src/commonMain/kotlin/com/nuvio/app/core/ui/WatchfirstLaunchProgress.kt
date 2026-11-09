package com.nuvio.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

private val LaunchGreen = Color(0xFF39FF14)
private val LaunchTrack = Color(0x1AFFFFFF)

/**
 * WATCHFIRST start-up indicator: a hairline track with one green light travelling across it.
 * Replaces the generic rotating arc so the first screen is the brand's own, not the upstream one.
 */
@Composable
internal fun WatchfirstLaunchProgress(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "watchfirst_launch")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "watchfirst_launch_sweep",
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = LaunchTrack, cornerRadius = radius)
        val lightWidth = size.width * 0.32f
        val left = -lightWidth + sweep * (size.width + lightWidth)
        clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, LaunchGreen, Color.Transparent),
                    startX = left,
                    endX = left + lightWidth,
                ),
                topLeft = Offset(left, 0f),
                size = Size(lightWidth, size.height),
                cornerRadius = radius,
            )
        }
    }
}
