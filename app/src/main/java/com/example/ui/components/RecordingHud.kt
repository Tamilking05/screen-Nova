package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RecordingState
import java.util.Locale

@Composable
fun RecordingHud(
    state: RecordingState,
    durationMs: Long,
    amplitude: Float,
    isMicActive: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = state == RecordingState.PAUSED
    val isRecording = state == RecordingState.RECORDING

    // Pulsing red dot animation
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            pulseScale.animateTo(
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formattedDuration = if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    Surface(
        modifier = modifier
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
            .testTag("recording_hud_bar"),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live red dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(if (isPaused) 1f else pulseScale.value)
                    .background(
                        if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444),
                        CircleShape
                    )
            )

            // Duration timer text
            Text(
                text = formattedDuration,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("recording_duration_text")
            )

            // Status label if paused
            if (isPaused) {
                Text(
                    text = "PAUSED",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Audio wave bar when mic is active
            if (isMicActive) {
                AudioWaveformBar(
                    amplitude = amplitude,
                    isActive = isRecording,
                    barCount = 5,
                    maxHeight = 18.dp,
                    barColor = if (isPaused) Color.Gray else Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Pause / Resume button
            IconButton(
                onClick = if (isPaused) onResume else onPause,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .testTag("pause_resume_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Stop button (prominent red)
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFEF4444), CircleShape)
                    .testTag("stop_recording_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Recording",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Cancel / Discard
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .testTag("cancel_recording_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Discard Recording",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
