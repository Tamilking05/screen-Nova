package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RecordingState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FloatingRecordingWidget(
    state: RecordingState,
    durationMs: Long,
    isMicActive: Boolean,
    isWebcamActive: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onToggleBrush: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleWebcam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(450f) }
    var isExpanded by remember { mutableStateOf(false) }
    var showToolsMenu by remember { mutableStateOf(false) }

    val isPaused = state == RecordingState.PAUSED
    val isRecording = state == RecordingState.RECORDING

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            pulseScale.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    val totalSeconds = durationMs / 1000
    val formattedDuration = String.format(Locale.US, "%02d:%02d", (totalSeconds % 3600) / 60, totalSeconds % 60)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_recording_widget")
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tools Quick Menu popover
            AnimatedVisibility(visible = showToolsMenu, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.96f),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Quick Tools",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Mic toggle
                            IconButton(
                                onClick = onToggleMic,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isMicActive) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Toggle Mic",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Webcam PIP toggle
                            IconButton(
                                onClick = onToggleWebcam,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isWebcamActive) Color(0xFF06B6D4) else Color.Gray.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isWebcamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle FaceCam",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Screenshot
                            IconButton(
                                onClick = {
                                    showToolsMenu = false
                                    onTakeScreenshot()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF97316), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Screenshot",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Widget Capsule
            Surface(
                color = Color(0xFF0B1120).copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444)))
                ),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating Timer Orb (matches Image 1 & 7 circular badge)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                        Color(0xFFB91C1C)
                                    )
                                ),
                                CircleShape
                            )
                            .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Expanded Controls: Pause, Stop, Brush, Tools
                    if (isExpanded) {
                        // Pause / Resume
                        IconButton(
                            onClick = onPauseResume,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("floating_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause / Resume",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Stop Button (Red circle with square icon)
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFEF4444), CircleShape)
                                .testTag("floating_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Brush / Doodle tool button (matches Image 1)
                        IconButton(
                            onClick = onToggleBrush,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF97316).copy(alpha = 0.25f), CircleShape)
                                .testTag("floating_brush_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Screen Brush",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Screenshot Tool (matches Image 1 & 6)
                        IconButton(
                            onClick = onTakeScreenshot,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF06B6D4).copy(alpha = 0.25f), CircleShape)
                                .testTag("floating_screenshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "One Tap Screenshot",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tools / Wrench popover (matches Image 1)
                        IconButton(
                            onClick = { showToolsMenu = !showToolsMenu },
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (showToolsMenu) Color(0xFFF97316) else Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("floating_tools_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Tools",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Collapse (X)
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Collapse",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
