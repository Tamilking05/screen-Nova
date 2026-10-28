package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "screen_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var outputFile: File? = null
    private var isRecording = false
    private var isPaused = false
    private var startTimeMs = 0L

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var amplitudeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startForegroundServiceWithNotification("Screen recording in progress...")
                    startRecording(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification(statusText: String) {
        val notification = buildNotification(statusText, isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(statusText: String, isPaused: Boolean): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 2, toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeLabel = if (isPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenNova Studio")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, pauseResumeLabel, togglePendingIntent)
            .addAction(android.R.drawable.ic_menu_camera, "Tools", appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows screen recording controls and status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startRecording(resultCode: Int, resultData: Intent) {
        try {
            val settings = ScreenRecorderManager.settings.value
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            // Determine dimensions from settings or screen
            val targetWidth = settings.videoQuality.width
            val targetHeight = settings.videoQuality.height

            // Adjust aspect ratio to match display orientation
            val screenIsPortrait = metrics.heightPixels > metrics.widthPixels
            val width = if (screenIsPortrait) minOf(targetWidth, targetHeight) else maxOf(targetWidth, targetHeight)
            val height = if (screenIsPortrait) maxOf(targetWidth, targetHeight) else minOf(targetWidth, targetHeight)
            val dpi = metrics.densityDpi

            val movieDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
            if (!movieDir.exists()) movieDir.mkdirs()
            val file = File(movieDir, "ScreenRecord_${System.currentTimeMillis()}.mp4")
            outputFile = file

            // Configure MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                if (settings.isMicEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (settings.isMicEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                }
                setVideoSize(width, height)
                setVideoFrameRate(settings.videoQuality.fps)
                setVideoEncodingBitRate(settings.videoQuality.bitrate)
                setOutputFile(file.absolutePath)
                prepare()
            }

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped by system")
                    stopRecording()
                }
            }, null)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenNovaDisplay",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            isRecording = true
            isPaused = false
            startTimeMs = System.currentTimeMillis()

            ScreenRecorderManager.onServiceRecordingStarted(file)

            // Audio amplitude monitoring
            if (settings.isMicEnabled) {
                startAmplitudeJob()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            stopRecording()
        }
    }

    private fun startAmplitudeJob() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive && isRecording && !isPaused) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize amplitude between 0.0 and 1.0 (maxAmp is up to 32767)
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    ScreenRecorderManager.updateAmplitude(normalized)
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }

    private fun pauseRecording() {
        if (isRecording && !isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    isPaused = true
                    amplitudeJob?.cancel()
                    val notification = buildNotification("Recording paused", isPaused = true)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    private fun resumeRecording() {
        if (isRecording && isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    isPaused = false
                    val notification = buildNotification("Screen recording in progress...", isPaused = false)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, notification)
                    if (ScreenRecorderManager.settings.value.isMicEnabled) {
                        startAmplitudeJob()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    private fun stopRecording() {
        amplitudeJob?.cancel()
        var duration = 0L
        if (startTimeMs > 0L) {
            duration = System.currentTimeMillis() - startTimeMs
        }

        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "mediaRecorder.stop() failed or stopped early: ${e.message}")
        }

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null

        isRecording = false
        isPaused = false

        outputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                ScreenRecorderManager.onServiceRecordingComplete(this, file, duration)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
