package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewScreen(
    recording: RecordingEntity,
    onBack: () -> Unit,
    onRename: (RecordingEntity, String) -> Unit,
    onDelete: (RecordingEntity) -> Unit,
    onEditRecording: (RecordingEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(recording.durationMs.toInt()) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // Dialogs
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(recording.title) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Track playback progress
    LaunchedEffect(isPlaying, isDraggingSlider) {
        while (isActive && isPlaying && !isDraggingSlider) {
            videoViewInstance?.let { vv ->
                if (vv.isPlaying) {
                    val pos = vv.currentPosition
                    currentPositionMs = pos
                    val dur = vv.duration
                    if (dur > 0) {
                        durationMs = dur
                        sliderPosition = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    }
                } else if (!vv.isPlaying && currentPositionMs > 0 && currentPositionMs >= durationMs - 500) {
                    isPlaying = false
                }
            }
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    val totalSec = (durationMs / 1000).coerceAtLeast(1)
    val curSec = currentPositionMs / 1000
    val timeLabel = String.format(Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60)
    val mbStr = String.format(Locale.US, "%.2f MB", recording.fileSizeBytes / (1024f * 1024f))
    val dateStr = SimpleDateFormat("MMMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(recording.createdAt))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = recording.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$dateStr · $mbStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    renameInput = recording.title
                    showRenameDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Player Container
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("video_player_container")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val file = File(recording.filePath)
                                if (file.exists()) {
                                    setVideoURI(Uri.fromFile(file))
                                }
                                setOnPreparedListener { mp ->
                                    durationMs = mp.duration
                                    mp.isLooping = false
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPositionMs = durationMs
                                    sliderPosition = 1f
                                }
                                videoViewInstance = this
                            }
                        }
                    )

                    // Big center play button if paused
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    videoViewInstance?.let { vv ->
                                        if (currentPositionMs >= durationMs - 500) {
                                            vv.seekTo(0)
                                            currentPositionMs = 0
                                        }
                                        vv.start()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize().testTag("center_play_button")
                            ) {
                                Icon(
                                    imageVector = if (currentPositionMs >= durationMs - 500) Icons.Default.Replay else Icons.Default.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Player Controls Bar (Linear / Loom design)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Seekbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { newFrac ->
                                isDraggingSlider = true
                                sliderPosition = newFrac
                                currentPositionMs = (newFrac * durationMs).toInt()
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                videoViewInstance?.seekTo(currentPositionMs)
                            },
                            modifier = Modifier.weight(1f).testTag("video_progress_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Bottom Row: Time and Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeLabel,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Replay 10s
                            IconButton(
                                onClick = {
                                    videoViewInstance?.let { vv ->
                                        val newPos = (vv.currentPosition - 10_000).coerceAtLeast(0)
                                        vv.seekTo(newPos)
                                        currentPositionMs = newPos
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                            }

                            // Play/Pause
                            IconButton(
                                onClick = {
                                    videoViewInstance?.let { vv ->
                                        if (isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            if (currentPositionMs >= durationMs - 500) {
                                                vv.seekTo(0)
                                                currentPositionMs = 0
                                            }
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("playback_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Forward 10s
                            IconButton(
                                onClick = {
                                    videoViewInstance?.let { vv ->
                                        val newPos = (vv.currentPosition + 10_000).coerceAtMost(durationMs)
                                        vv.seekTo(newPos)
                                        currentPositionMs = newPos
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                            }
                        }
                    }
                }
            }

            // Quick Actions: Trim & Studio Editor (Images 3 & 4), Share, Download
            Button(
                onClick = { onEditRecording(recording) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("preview_edit_studio_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trim, Split & AI Auto Captions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        shareVideo(context, recording)
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("preview_share_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Video")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Video file saved at: ${recording.filePath}")
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("preview_export_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download MP4")
                }
            }

            // Technical Specs Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Technical Metadata",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    SpecRow("Resolution", "${recording.width} × ${recording.height} (${recording.height}p)")
                    SpecRow("Frame Rate", "${recording.fps} FPS")
                    SpecRow("Video Codec", "H.264 / AVC (MPEG-4)")
                    SpecRow("Audio Track", if (recording.hasAudio) "AAC Stereo 44.1kHz" else "Muted")
                    SpecRow("Capture Source", recording.captureSource)
                    SpecRow("Webcam FaceCam", if (recording.hasWebcam) "Included (PIP)" else "Disabled")
                    SpecRow("Total File Size", mbStr)
                    SpecRow("Storage Location", recording.filePath)
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRename(recording, renameInput.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this recording?") },
            text = { Text("This recording will be permanently deleted from device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(recording)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun shareVideo(context: Context, recording: RecordingEntity) {
    try {
        val file = File(recording.filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Recording"))
        }
    } catch (_: Exception) {}
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
