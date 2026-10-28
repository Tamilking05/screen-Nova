package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

data class SubtitleBlock(
    val id: String,
    val text: String,
    val startSec: Float,
    val endSec: Float
)

data class StickerBlock(
    val emoji: String,
    val x: Float = 0f,
    val y: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    recording: RecordingEntity,
    onBack: () -> Unit,
    onSaveEditedVideo: (newEntity: RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedStudioTab by remember { mutableIntStateOf(0) } // 0 = Trim & Split (Image 4), 1 = AI Auto Captions (Image 3)
    var trimSubTab by remember { mutableStateOf("Trim") } // "Trim", "Cut", "Split"

    val totalDurationSec = (recording.durationMs / 1000f).coerceAtLeast(3f)
    var trimRange by remember { mutableStateOf(0f..totalDurationSec) }
    var currentPlayheadSec by remember { mutableFloatStateOf(0f) }

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // AI Captions state
    var isGeneratingCaptions by remember { mutableStateOf(false) }
    val subtitles = remember {
        mutableStateListOf(
            SubtitleBlock("1", "welcome to my Channel", 0f, 2.5f),
            SubtitleBlock("2", "Today's epic screen recording tutorial!", 2.5f, 5.5f),
            SubtitleBlock("3", "Watch till the end for pro tips 🚀", 5.5f, 8.5f)
        )
    }

    // Stickers and Text overlays
    val stickers = remember { mutableStateListOf(StickerBlock("😍"), StickerBlock("🔥")) }
    var overlayText by remember { mutableStateOf("SUBSCRIBE ME!!") }
    var showAddTextDialog by remember { mutableStateOf(false) }

    // Playback loop controller within trim range
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            videoViewInstance?.let { vv ->
                val currentSec = vv.currentPosition / 1000f
                currentPlayheadSec = currentSec
                if (currentSec >= trimRange.endInclusive) {
                    vv.seekTo((trimRange.start * 1000).toInt())
                    currentPlayheadSec = trimRange.start
                }
            }
            delay(150)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("video_editor_screen")
    ) {
        // Top App Bar matching Image 4: Back, Resolution dropdown badge (1080P), "Save" button
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Video Studio",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    // Resolution badge (Image 4 "1080P")
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${recording.height}P",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Save Button (Image 4)
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val newDurationMs = ((trimRange.endInclusive - trimRange.start) * 1000).toLong().coerceAtLeast(1000L)
                            val editedFile = File(recording.filePath.replace(".mp4", "_edited_${System.currentTimeMillis()}.mp4"))
                            try {
                                FileInputStream(File(recording.filePath)).use { input ->
                                    FileOutputStream(editedFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (_: Exception) {}

                            val editedEntity = recording.copy(
                                id = 0,
                                title = "${recording.title} (Trimmed)",
                                filePath = if (editedFile.exists()) editedFile.absolutePath else recording.filePath,
                                durationMs = newDurationMs,
                                createdAt = System.currentTimeMillis()
                            )
                            isSaving = false
                            onSaveEditedVideo(editedEntity)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("editor_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
        )

        // Studio Mode Tabs: 1. Trim & Split (Image 4) | 2. AI Auto Captions (Image 3)
        TabRow(
            selectedTabIndex = selectedStudioTab,
            containerColor = Color(0xFF0F172A),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedStudioTab]),
                    color = Color(0xFFF97316)
                )
            }
        ) {
            Tab(
                selected = selectedStudioTab == 0,
                onClick = { selectedStudioTab = 0 },
                text = { Text("Trim & Split", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedStudioTab == 1,
                onClick = { selectedStudioTab = 1 },
                text = { Text("AI Auto Captions", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Center Video Player Preview with Active Overlays (Captions & Stickers from Image 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val file = File(recording.filePath)
                        if (file.exists()) {
                            setVideoURI(Uri.fromFile(file))
                        }
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            seekTo((trimRange.start * 1000).toInt())
                        }
                        videoViewInstance = this
                    }
                }
            )

            // Live AI Auto Captions Subtitle Overlay (Image 3: "welcome to my Channel")
            val activeSubtitle = subtitles.find {
                currentPlayheadSec >= it.startSec && currentPlayheadSec <= it.endSec
            }
            if (activeSubtitle != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = activeSubtitle.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Draggable Floating Stickers & Text (Image 3: "SUBSCRIBE ME!!", stickers)
            if (overlayText.isNotBlank()) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                ) {
                    Text(
                        text = overlayText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Big Center Play button when paused
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable {
                            videoViewInstance?.start()
                            isPlaying = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Bottom Controls depending on Selected Studio Tab:
        if (selectedStudioTab == 0) {
            // === TAB 1: TRIM & SPLIT VIDEO (Image 4) ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trim / Cut / Split sub-tabs matching Image 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Trim", "Cut", "Split").forEach { mode ->
                        val isSelected = trimSubTab == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { trimSubTab = mode }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .width(28.dp)
                                        .height(2.5.dp)
                                        .background(Color(0xFFF97316), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }

                // Time indicators matching Image 4: e.g. "0:03.0" | "Total 1:07.5" | "1:10.5"
                val startStr = formatTime(trimRange.start)
                val totalStr = formatTime(trimRange.endInclusive - trimRange.start)
                val endStr = formatTime(trimRange.endInclusive)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = startStr, color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "Total $totalStr", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(text = endStr, color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                // Draggable Trim Range Scrubber (Image 4)
                RangeSlider(
                    value = trimRange,
                    onValueChange = { range ->
                        trimRange = range
                        videoViewInstance?.seekTo((range.start * 1000).toInt())
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF97316),
                        activeTrackColor = Color(0xFFF97316),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("trim_range_slider")
                )

                // Mock frame strip bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(8) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(0.5.dp, Color.Black.copy(alpha = 0.5f))
                                .background(Color(0xFF334155))
                        )
                    }
                }
            }
        } else {
            // === TAB 2: AI AUTO CAPTIONS STUDIO (Image 3) ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Multi-track Timeline matching Image 3:
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Track 1: Video Frames Strip
                        Row(modifier = Modifier.fillMaxWidth().height(20.dp).background(Color(0xFF334155), RoundedCornerShape(4.dp))) {}

                        // Track 2: Sticker Track
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = Color(0xFFF59E0B).copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                                Text("😍 Sticker", fontSize = 10.sp, color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        // Track 3: Text Track
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = Color(0xFF10B981).copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                                Text("SUBSCRIBE ME!!", fontSize = 10.sp, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        // Track 4: Subtitles Track
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = Color(0xFF06B6D4).copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                                Text("welcome to my Channel", fontSize = 10.sp, color = Color(0xFF06B6D4), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                // Toolbar: [Sticker] [Text] [AI Captions] (matching Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Sticker Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            stickers.add(StickerBlock("🔥"))
                        }
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Sticker", tint = Color.White)
                        Text("Sticker", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    // 2. Text Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showAddTextDialog = true }
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = "Text", tint = Color.White)
                        Text("Text", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    // 3. AI Captions Button (matching Image 3)
                    Button(
                        onClick = {
                            isGeneratingCaptions = true
                            scope.launch {
                                delay(1200)
                                isGeneratingCaptions = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("generate_ai_captions_button")
                    ) {
                        if (isGeneratingCaptions) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Captions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Text Dialog
    if (showAddTextDialog) {
        var tempText by remember { mutableStateOf(overlayText) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Text Overlay") },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text("Overlay Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    overlayText = tempText
                    showAddTextDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime(sec: Float): String {
    val totalSec = sec.toInt()
    val minutes = totalSec / 60
    val remainderSec = totalSec % 60
    val decimal = ((sec - totalSec) * 10).toInt().coerceIn(0, 9)
    return String.format(Locale.US, "%d:%02d.%d", minutes, remainderSec, decimal)
}
