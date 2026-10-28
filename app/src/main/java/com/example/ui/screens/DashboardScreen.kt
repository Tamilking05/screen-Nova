package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingEntity
import com.example.service.CaptureSource
import com.example.service.RecordingSettings
import com.example.service.ScreenRecorderManager
import com.example.service.VideoQuality
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.GameStudioWorkspaceView
import com.example.util.DemoVideoSynthesizer
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    settings: RecordingSettings,
    recordings: List<RecordingEntity>,
    adsWatchedCount: Int = 0,
    onStartRecordingClick: () -> Unit,
    onRecordingSelect: (RecordingEntity) -> Unit,
    onViewAllClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onTakeScreenshot: () -> Unit = {},
    onToggleBrush: () -> Unit = {},
    onOpenEditorForLatest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculate quick stats
    val totalCount = recordings.size
    val totalDurationSeconds = recordings.sumOf { it.durationMs } / 1000
    val totalDurationMins = totalDurationSeconds / 60
    val totalBytes = recordings.sumOf { it.fileSizeBytes }
    val storageMb = String.format(Locale.US, "%.1f MB", totalBytes / (1024f * 1024f))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "ScreenNova Logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
            Column {
                Text(
                    text = "ScreenNova",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Home · Studio Screen Recorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stats Overview Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Recordings",
                value = "$totalCount",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Time",
                value = "${totalDurationMins}m",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Storage",
                value = storageMb,
                modifier = Modifier.weight(1f)
            )
        }


        // Section: Select Capture Source
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Capture Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CaptureSourceCard(
                    title = "Entire Screen",
                    subtitle = "Display & system",
                    icon = Icons.Default.Window,
                    isSelected = settings.captureSource == CaptureSource.FULL_SCREEN,
                    onClick = {
                        ScreenRecorderManager.updateSettings { it.copy(captureSource = CaptureSource.FULL_SCREEN) }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "source_full_screen"
                )
                CaptureSourceCard(
                    title = "Game & Stream",
                    subtitle = "60FPS Gameplay",
                    icon = Icons.Default.PlayArrow,
                    isSelected = settings.captureSource == CaptureSource.GAME_CAPTURE,
                    onClick = {
                        ScreenRecorderManager.updateSettings { it.copy(captureSource = CaptureSource.GAME_CAPTURE) }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "source_game_capture"
                )
            }
        }

        // If Game & Stream selected, show high-performance gaming studio workspace
        if (settings.captureSource == CaptureSource.GAME_CAPTURE) {
            GameStudioWorkspaceView(modifier = Modifier.fillMaxWidth())
        }

        // Device Controls Card (Mic, Webcam, System Audio)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Recording Inputs & Devices",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Microphone Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (settings.isMicEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (settings.isMicEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Microphone Audio",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (settings.isMicEnabled) {
                                AudioWaveformBar(
                                    isActive = true,
                                    amplitude = 0.6f,
                                    barCount = 6,
                                    maxHeight = 14.dp,
                                    barColor = Color(0xFF10B981)
                                )
                            } else {
                                Text(
                                    text = "Muted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Switch(
                        checked = settings.isMicEnabled,
                        onCheckedChange = { enabled ->
                            ScreenRecorderManager.updateSettings { it.copy(isMicEnabled = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("toggle_mic_switch")
                    )
                }

                // Webcam Overlay Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (settings.isWebcamEnabled) Color(0xFF06B6D4).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.isWebcamEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = if (settings.isWebcamEnabled) Color(0xFF06B6D4) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Webcam FaceCam Overlay",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (settings.isWebcamEnabled) "PIP floating bubble active" else "Camera disabled",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.isWebcamEnabled,
                        onCheckedChange = { enabled ->
                            ScreenRecorderManager.updateSettings { it.copy(isWebcamEnabled = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF06B6D4)
                        ),
                        modifier = Modifier.testTag("toggle_webcam_switch")
                    )
                }

                // System Audio Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (settings.isSystemAudioEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (settings.isSystemAudioEnabled) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "System Audio",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (settings.isSystemAudioEnabled) "Internal device audio playback" else "Off",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.isSystemAudioEnabled,
                        onCheckedChange = { enabled ->
                            ScreenRecorderManager.updateSettings { it.copy(isSystemAudioEnabled = enabled) }
                        },
                        modifier = Modifier.testTag("toggle_system_audio_switch")
                    )
                }

                // Floating Window Controls Toggle (Image 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF97316).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Floating Window Controls",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "On-screen widget & quick actions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.isFloatingControlEnabled,
                        onCheckedChange = { enabled ->
                            ScreenRecorderManager.updateSettings { it.copy(isFloatingControlEnabled = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF97316)
                        ),
                        modifier = Modifier.testTag("toggle_floating_controls_switch")
                    )
                }

                // Screen Doodle Brush Toggle (Images 1 & 7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Screen Brush Overlay",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Doodle & point on screen while recording",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.isBrushEnabled,
                        onCheckedChange = { enabled ->
                            ScreenRecorderManager.updateSettings { it.copy(isBrushEnabled = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("toggle_screen_brush_switch")
                    )
                }
            }
        }

        // Quality Resolution Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoQuality.values().forEach { quality ->
                    FilterChip(
                        selected = settings.videoQuality == quality,
                        onClick = {
                            ScreenRecorderManager.updateSettings { it.copy(videoQuality = quality) }
                        },
                        label = { Text(quality.label, fontSize = 12.sp) },
                        leadingIcon = if (settings.videoQuality == quality) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Screen Recording Time Allowance Card
        // Condition: 1 ad watch = 1 min, 3 ads watch = 3 mins, 5 ads watch = 5 mins, 10 ads watch = Unlimited
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (adsWatchedCount >= 10) Color(0xFF10B981) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
                .testTag("recording_allowance_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recording Time Allowance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (adsWatchedCount >= 10) "👑 Unlimited Recording Unlocked!" else "Watch ad before recording to level up time",
                            fontSize = 11.sp,
                            color = if (adsWatchedCount >= 10) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (adsWatchedCount >= 10) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$adsWatchedCount / 10 Ads",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (adsWatchedCount >= 10) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 4 Milestones: 1 Ad (1m) -> 3 Ads (3m) -> 5 Ads (5m) -> 10 Ads (Unlimited)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MilestoneChip(
                        adsRequired = 1,
                        durationLabel = "1 Min",
                        currentAds = adsWatchedCount,
                        modifier = Modifier.weight(1f)
                    )
                    MilestoneChip(
                        adsRequired = 3,
                        durationLabel = "3 Min",
                        currentAds = adsWatchedCount,
                        modifier = Modifier.weight(1f)
                    )
                    MilestoneChip(
                        adsRequired = 5,
                        durationLabel = "5 Min",
                        currentAds = adsWatchedCount,
                        modifier = Modifier.weight(1f)
                    )
                    MilestoneChip(
                        adsRequired = 10,
                        durationLabel = "Unlimited",
                        currentAds = adsWatchedCount,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Primary Start Recording Button (Only ads integrate button on app: plays ad before starting recording)
        val currentLimitLabel = when {
            adsWatchedCount >= 10 -> "Unlimited Limit"
            adsWatchedCount >= 5 -> "5 Mins Limit"
            adsWatchedCount >= 3 -> "3 Mins Limit"
            adsWatchedCount >= 1 -> "1 Min Limit"
            else -> "Watch 1st Ad for 1 Min"
        }

        Button(
            onClick = onStartRecordingClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("start_recording_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.White, CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Start Screen Recording",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Watch Ad & Start Recording ($currentLimitLabel)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Quick Feature Shortcuts from App Images (Image 6 Screenshot & Images 3/4 Video Studio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color(0xFFF97316).copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTakeScreenshot() }
                    .testTag("dashboard_screenshot_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Screenshot",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316),
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                color = Color(0xFF06B6D4).copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenEditorForLatest() }
                    .testTag("dashboard_studio_editor_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Trim & AI Captions",
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trim & Captions",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Recent Recordings Section
        if (recordings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Recordings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All (${recordings.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onViewAllClick() }
                            .padding(4.dp)
                    )
                }

                // Show top 2 recordings
                recordings.take(2).forEach { recording ->
                    RecentRecordingItem(
                        recording = recording,
                        onClick = { onRecordingSelect(recording) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MilestoneChip(
    adsRequired: Int,
    durationLabel: String,
    currentAds: Int,
    modifier: Modifier = Modifier
) {
    val isAchieved = currentAds >= adsRequired
    val activeBorder = if (isAchieved) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val activeBg = if (isAchieved) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Surface(
        color = activeBg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(if (isAchieved) 1.5.dp else 1.dp, activeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isAchieved) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = "$adsRequired Ad${if (adsRequired > 1) "s" else ""}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAchieved) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = durationLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isAchieved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CaptureSourceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentRecordingItem(
    recording: RecordingEntity,
    onClick: () -> Unit
) {
    val totalSec = recording.durationMs / 1000
    val durationStr = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    val mbStr = String.format(Locale.US, "%.1f MB", recording.fileSizeBytes / (1024f * 1024f))

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail placeholder / play icon
            Box(
                modifier = Modifier
                    .size(64.dp, 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = durationStr,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${recording.width}x${recording.height} · $mbStr · ${recording.captureSource}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
