package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformBar(
    modifier: Modifier = Modifier,
    amplitude: Float = 0f,
    isActive: Boolean = true,
    barCount: Int = 7,
    maxHeight: Dp = 24.dp,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = when (i % 4) {
                0 -> 0.4f
                1 -> 0.85f
                2 -> 1.0f
                else -> 0.6f
            }

            val targetHeightFraction = if (isActive) {
                if (amplitude > 0.05f) {
                    ((amplitude * factor) + 0.15f).coerceIn(0.15f, 1f)
                } else {
                    ((pulseAnim * factor) + 0.15f).coerceIn(0.15f, 0.7f)
                }
            } else {
                0.15f
            }

            val animatedHeight = remember { Animatable(0.2f) }
            LaunchedEffect(targetHeightFraction, isActive) {
                animatedHeight.animateTo(
                    targetValue = targetHeightFraction,
                    animationSpec = tween(120, easing = FastOutSlowInEasing)
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(maxHeight * animatedHeight.value)
                    .background(
                        if (isActive) barColor else barColor.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
