package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.RecordingEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    recordings: List<RecordingEntity>,
    onSelectRecording: (RecordingEntity) -> Unit,
    onRenameRecording: (RecordingEntity, String) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onStartRecordingClick: () -> Unit,
    onEditRecording: (RecordingEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(false) }

    // Dialogs state
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var newTitleText by remember { mutableStateOf("") }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForInfo by remember { mutableStateOf<RecordingEntity?>(null) }

    val filteredList = recordings.filter { recording ->
        val matchesQuery = recording.title.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Full Screen" -> recording.captureSource == "FULL_SCREEN"
            "Game & Stream" -> recording.captureSource == "GAME_CAPTURE"
            "HD" -> recording.height >= 720
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recording Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${recordings.size} total videos saved on device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier.testTag("toggle_library_layout_button")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Switch View"
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search recordings by name...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Full Screen", "Game & Stream", "HD").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        // Recordings List or Grid
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No recordings match '$searchQuery'" else "No recordings yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capture your entire screen or browser tab with high quality video and audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onStartRecordingClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("empty_state_record_button")
                    ) {
                        Text("Record First Video")
                    }
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { rec ->
                        RecordingGridItem(
                            recording = rec,
                            onClick = { onSelectRecording(rec) },
                            onRename = {
                                recordingToRename = rec
                                newTitleText = rec.title
                            },
                            onDelete = { recordingToDelete = rec },
                            onShare = { shareRecording(context, rec) },
                            onInfo = { recordingForInfo = rec }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { rec ->
                        RecordingListItem(
                            recording = rec,
                            onClick = { onSelectRecording(rec) },
                            onRename = {
                                recordingToRename = rec
                                newTitleText = rec.title
                            },
                            onDelete = { recordingToDelete = rec },
                            onShare = { shareRecording(context, rec) },
                            onInfo = { recordingForInfo = rec }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    recordingToRename?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitleText.isNotBlank()) {
                            onRenameRecording(rec, newTitleText.trim())
                        }
                        recordingToRename = null
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    recordingToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording?") },
            text = { Text("Are you sure you want to delete '${rec.title}'? This will permanently remove the video file from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecording(rec)
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Details Modal
    recordingForInfo?.let { rec ->
        val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm:ss", Locale.getDefault())
        val mb = String.format(Locale.US, "%.2f MB", rec.fileSizeBytes / (1024f * 1024f))
        val totalSec = rec.durationMs / 1000
        val durationStr = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)

        AlertDialog(
            onDismissRequest = { recordingForInfo = null },
            title = { Text("Recording Specs") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${rec.title}", fontWeight = FontWeight.Bold)
                    Text("Duration: $durationStr (${rec.durationMs / 1000}s)")
                    Text("Resolution: ${rec.width} x ${rec.height}")
                    Text("Frame Rate: ${rec.fps} FPS")
                    Text("File Size: $mb")
                    Text("Source: ${rec.captureSource}")
                    Text("Audio: ${if (rec.hasAudio) "Yes (AAC)" else "No"}")
                    Text("FaceCam Overlay: ${if (rec.hasWebcam) "Yes" else "No"}")
                    Text("Created: ${dateFormat.format(Date(rec.createdAt))}")
                    Text("File Path: ${rec.filePath}", fontSize = 10.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { recordingForInfo = null }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun shareRecording(context: Context, recording: RecordingEntity) {
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
fun RecordingListItem(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit
) {
    val totalSec = recording.durationMs / 1000
    val durationStr = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    val mbStr = String.format(Locale.US, "%.1f MB", recording.fileSizeBytes / (1024f * 1024f))
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(recording.createdAt))

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .testTag("recording_item_${recording.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp, 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (recording.thumbnailPath != null) {
                    AsyncImage(
                        model = recording.thumbnailPath,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = durationStr, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = "$dateStr · $mbStr · ${recording.height}p",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRename, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RecordingGridItem(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit
) {
    val totalSec = recording.durationMs / 1000
    val durationStr = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    val mbStr = String.format(Locale.US, "%.1f MB", recording.fileSizeBytes / (1024f * 1024f))

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (recording.thumbnailPath != null) {
                    AsyncImage(
                        model = recording.thumbnailPath,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = durationStr, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = recording.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = "$mbStr · ${recording.height}p",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
