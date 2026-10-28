package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.RecordingEntity
import java.io.File
import java.util.Locale

@Composable
fun VideoSavedDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalSeconds = (recording.durationMs / 1000).coerceAtLeast(1)
    val durationStr = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    val mbStr = String.format(Locale.US, "%.1f MB", recording.fileSizeBytes / (1024f * 1024f))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444)))
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("video_saved_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Video Saved",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "$durationStr · ${recording.height}P · $mbStr",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Thumbnail with Glowing Play Button (Matches Image 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (recording.thumbnailPath != null) {
                        AsyncImage(
                            model = File(recording.thumbnailPath),
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    // Circular center Play button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
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

                // Share Section Header (Matches Image 2: "Easy to Share & Save")
                Text(
                    text = "Share to Social Media",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Quick Social Share Shortcuts (YouTube, Instagram, WhatsApp, Facebook, More - from Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SocialShareItem(
                        label = "YouTube",
                        color = Color(0xFFFF0000),
                        onClick = { shareToApp(context, recording, "com.google.android.youtube") }
                    )
                    SocialShareItem(
                        label = "Instagram",
                        color = Color(0xFFE1306C),
                        onClick = { shareToApp(context, recording, "com.instagram.android") }
                    )
                    SocialShareItem(
                        label = "WhatsApp",
                        color = Color(0xFF25D366),
                        onClick = { shareToApp(context, recording, "com.whatsapp") }
                    )
                    SocialShareItem(
                        label = "Facebook",
                        color = Color(0xFF1877F2),
                        onClick = { shareToApp(context, recording, "com.facebook.katana") }
                    )
                    SocialShareItem(
                        label = "More",
                        color = Color(0xFFF97316),
                        onClick = { shareGeneric(context, recording) }
                    )
                }

                // Action Buttons: Edit Video & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("dialog_edit_video_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trim & Edit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialShareItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shareToApp(context: Context, recording: RecordingEntity, targetPackage: String) {
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
                setPackage(targetPackage)
            }
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}
    // Fallback to generic share
    shareGeneric(context, recording)
}

private fun shareGeneric(context: Context, recording: RecordingEntity) {
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
