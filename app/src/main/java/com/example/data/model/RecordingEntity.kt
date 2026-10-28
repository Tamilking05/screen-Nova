package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val hasAudio: Boolean = true,
    val hasWebcam: Boolean = false,
    val captureSource: String = "FULL_SCREEN" // FULL_SCREEN, BROWSER_TAB
)
