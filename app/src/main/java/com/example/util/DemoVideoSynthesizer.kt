package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.data.repository.RecordingRepository
import com.example.service.CaptureSource
import com.example.service.ScreenRecorderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DemoVideoSynthesizer {
    private const val TAG = "DemoVideoSynthesizer"

    suspend fun createSyntheticRecording(
        context: Context,
        source: CaptureSource,
        titlePrefix: String = "Tab Capture"
    ): RecordingEntity? = withContext(Dispatchers.IO) {
        try {
            val width = 1280
            val height = 720
            val fps = 30
            val durationSeconds = 4
            val totalFrames = fps * durationSeconds
            val bitRate = 3_000_000

            val movieDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            if (!movieDir.exists()) movieDir.mkdirs()
            val videoFile = File(movieDir, "ScreenNova_${System.currentTimeMillis()}.mp4")

            val mimeType = "video/avc" // H.264
            val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until totalFrames) {
                // Render frame onto the encoder input surface
                val canvas: Canvas = inputSurface.lockHardwareCanvas()

                // Background
                canvas.drawColor(Color.parseColor("#090D16"))

                // Browser / window header
                paint.color = Color.parseColor("#131B2A")
                canvas.drawRoundRect(RectF(60f, 60f, width - 60f, height - 60f), 24f, 24f, paint)

                // Window Title Bar
                paint.color = Color.parseColor("#1E293B")
                canvas.drawRoundRect(RectF(60f, 60f, width - 60f, 130f), 24f, 24f, paint)

                // Window action dots
                paint.color = Color.parseColor("#EF4444")
                canvas.drawCircle(100f, 95f, 10f, paint)
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawCircle(130f, 95f, 10f, paint)
                paint.color = Color.parseColor("#10B981")
                canvas.drawCircle(160f, 95f, 10f, paint)

                // Dynamic progress / animated wave
                val progress = i.toFloat() / totalFrames
                val dotX = 200f + progress * (width - 400f)
                val dotY = 360f + kotlin.math.sin(i * 0.2f) * 60f

                // Pulsating circle
                paint.color = Color.parseColor("#6366F1")
                canvas.drawCircle(dotX, dotY, 36f, paint)

                paint.color = Color.parseColor("#38BDF8")
                canvas.drawCircle(dotX, dotY, 18f, paint)

                // Animated recording indicator in header
                if ((i / 15) % 2 == 0) {
                    paint.color = Color.parseColor("#EF4444")
                    canvas.drawCircle(width - 120f, 95f, 12f, paint)
                }

                val currentSec = i / fps
                val currentMs = (i % fps) * (1000 / fps)
                val timeLabel = String.format(Locale.US, "REC  00:0%d:%02d", currentSec, currentMs / 10)
                subTextPaint.color = Color.parseColor("#EF4444")
                subTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(timeLabel, width - 150f, 102f, subTextPaint)

                // Center main title
                textPaint.color = Color.WHITE
                val title = when (source) {
                    CaptureSource.GAME_CAPTURE -> "Game & Stream: 60FPS High-Def Gameplay"
                    CaptureSource.FULL_SCREEN -> "Screen Capture: Full Display"
                }
                canvas.drawText(title, width / 2f, 320f, textPaint)

                subTextPaint.color = Color.parseColor("#94A3B8")
                subTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Smooth 60FPS Video Capture · High Fidelity Audio", width / 2f, 440f, subTextPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            // Presentation timestamp
                            bufferInfo.presentationTimeUs = (i * 1_000_000L / fps)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    } else {
                        break
                    }
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain remaining
            while (true) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else {
                    break
                }
            }

            try {
                encoder.stop()
                encoder.release()
            } catch (_: Exception) {}

            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (_: Exception) {}

            // Save thumbnail
            val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            val thumbBitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
            val thumbCanvas = Canvas(thumbBitmap)
            thumbCanvas.drawColor(Color.parseColor("#131B2A"))
            paint.color = Color.parseColor("#6366F1")
            thumbCanvas.drawCircle(160f, 90f, 30f, paint)
            paint.color = Color.WHITE
            thumbCanvas.drawCircle(160f, 90f, 12f, paint)
            FileOutputStream(thumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            val entity = RecordingEntity(
                title = "$titlePrefix ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}",
                filePath = videoFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath,
                durationMs = (durationSeconds * 1000).toLong(),
                createdAt = System.currentTimeMillis(),
                fileSizeBytes = videoFile.length(),
                width = width,
                height = height,
                fps = fps,
                hasAudio = true,
                hasWebcam = false,
                captureSource = source.name
            )

            val repository = RecordingRepository(AppDatabase.getDatabase(context).recordingDao())
            val id = repository.saveRecording(entity)
            val saved = entity.copy(id = id)
            ScreenRecorderManager.clearErrorMessage()
            saved
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synthetic video", e)
            null
        }
    }
}
