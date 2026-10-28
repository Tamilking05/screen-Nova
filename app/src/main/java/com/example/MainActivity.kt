package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.ads.BannerAdView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.RecordingSettings
import com.example.service.RecordingState
import com.example.service.ScreenRecorderManager
import com.example.ui.components.CountdownOverlay
import com.example.ui.components.DraggableWebcamOverlay
import com.example.ui.components.FloatingRecordingWidget
import com.example.ui.components.RecordingHud
import com.example.ui.components.ScreenBrushOverlay
import com.example.ui.components.ScreenshotResultDialog
import com.example.ui.components.VideoSavedDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.screens.VideoPreviewScreen
import com.example.ads.AdManager
import com.example.ui.theme.ScreenNovaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob and show App Open Ad on launch with timeout fallback
        AdManager.initialize(this) {
            runOnUiThread {
                AdManager.loadAndShowAppOpenAd(this@MainActivity, timeoutMs = 3500L) {}
            }
        }

        setContent {
            val settings by viewModel.recorderSettings.collectAsStateWithLifecycle()

            ScreenNovaTheme(darkTheme = settings.isDarkMode) {
                MainAppContent(
                    viewModel = viewModel,
                    activity = this@MainActivity
                )
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel, activity: Activity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val settings by viewModel.recorderSettings.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val durationMs by viewModel.recordingDurationMs.collectAsStateWithLifecycle()
    val amplitude by viewModel.micAmplitude.collectAsStateWithLifecycle()
    val countdownRemaining by viewModel.countdownRemaining.collectAsStateWithLifecycle()
    val selectedRecording by viewModel.selectedRecording.collectAsStateWithLifecycle()
    val adsWatchedCount by viewModel.adsWatchedCount.collectAsStateWithLifecycle()
    val savedVideoForDialog by viewModel.savedVideoForDialog.collectAsStateWithLifecycle()
    val latestScreenshotPath by viewModel.latestScreenshotPath.collectAsStateWithLifecycle()

    var isBrushActive by remember { mutableStateOf(false) }

    val isInterstitialReady by AdManager.isInterstitialReady.collectAsStateWithLifecycle()
    val isRewardedReady by AdManager.isRewardedReady.collectAsStateWithLifecycle()
    val isAppOpenReady by AdManager.isAppOpenReady.collectAsStateWithLifecycle()
    val isNativeReady by AdManager.isNativeAdReady.collectAsStateWithLifecycle()

    // Back handling
    if (currentScreen != ScreenState.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(ScreenState.DASHBOARD)
        }
    }

    // MediaProjection system capture launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRealRecording(context, result.resultCode, result.data!!)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Screen capture permission was cancelled.")
            }
        }
    }

    // Permission launcher (Audio, Camera, Notifications)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
        val camGranted = permissions[Manifest.permission.CAMERA] ?: true

        if (!micGranted && settings.isMicEnabled) {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission denied. Recording will be muted.")
            }
        }

        // Proceed to screen capture prompt
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("MediaProjection is not supported on this device.")
            }
        }
    }

    val launchRecordingIntent = {
        val neededPermissions = mutableListOf<String>()
        if (settings.isMicEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (settings.isWebcamEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (neededPermissions.isNotEmpty()) {
            permissionsLauncher.launch(neededPermissions.toTypedArray())
        } else {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager != null) {
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        }
    }

    // Interstitial Video Ad before starting recording:
    // User watches ad to get recording allowance (1 ad = 1 min, 3 ads = 3 min, 5 ads = 5 min, 10 ads = Unlimited)
    val onStartRecordingRequested = {
        AdManager.loadAndShowInterstitialAd(activity, timeoutMs = 3000L) {
            val newCount = viewModel.incrementAdsWatched(context)
            val unlockedMsg = when {
                newCount >= 10 -> "👑 10 Ads Watched! Unlimited Screen Recording Unlocked!"
                newCount >= 5 -> "🎉 5 Ads Watched! 5 Minutes Screen Recording Active!"
                newCount >= 3 -> "🎉 3 Ads Watched! 3 Minutes Screen Recording Active!"
                else -> "🎉 1 Ad Watched! 1 Minute Screen Recording Active!"
            }
            scope.launch {
                snackbarHostState.showSnackbar(unlockedMsg)
            }
            launchRecordingIntent()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Persistent Google AdMob Banner Ad (Shown across ANY page visit)
                BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentScreen != ScreenState.PREVIEW && currentScreen != ScreenState.EDITOR) {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == ScreenState.DASHBOARD,
                            onClick = { viewModel.navigateTo(ScreenState.DASHBOARD) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenState.LIBRARY,
                            onClick = { viewModel.navigateTo(ScreenState.LIBRARY) },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Library") },
                            label = { Text("Library (${recordings.size})") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )
                        NavigationBarItem(
                            selected = currentScreen == ScreenState.SETTINGS,
                            onClick = { viewModel.navigateTo(ScreenState.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Routing
            when (currentScreen) {
                ScreenState.DASHBOARD -> {
                    DashboardScreen(
                        settings = settings,
                        recordings = recordings,
                        adsWatchedCount = adsWatchedCount,
                        onStartRecordingClick = onStartRecordingRequested,
                        onRecordingSelect = { recording ->
                            viewModel.selectRecordingForPreview(recording)
                        },
                        onViewAllClick = {
                            viewModel.navigateTo(ScreenState.LIBRARY)
                        },
                        onOpenSettingsClick = {
                            viewModel.navigateTo(ScreenState.SETTINGS)
                        },
                        onToggleDarkMode = {
                            viewModel.toggleDarkMode()
                        },
                        onTakeScreenshot = {
                            viewModel.takeScreenshot(context)
                        },
                        onToggleBrush = {
                            isBrushActive = !isBrushActive
                        },
                        onOpenEditorForLatest = {
                            val latest = recordings.firstOrNull()
                            if (latest != null) {
                                viewModel.openEditorForRecording(latest)
                            } else {
                                viewModel.takeScreenshot(context)
                            }
                        }
                    )
                }
                ScreenState.LIBRARY -> {
                    LibraryScreen(
                        recordings = recordings,
                        onSelectRecording = { recording ->
                            viewModel.selectRecordingForPreview(recording)
                        },
                        onRenameRecording = { recording, newTitle ->
                            viewModel.renameRecording(recording, newTitle)
                        },
                        onDeleteRecording = { recording ->
                            viewModel.deleteRecording(recording)
                        },
                        onStartRecordingClick = onStartRecordingRequested,
                        onEditRecording = { recording ->
                            viewModel.openEditorForRecording(recording)
                        }
                    )
                }
                ScreenState.PREVIEW -> {
                    selectedRecording?.let { rec ->
                        VideoPreviewScreen(
                            recording = rec,
                            onBack = {
                                viewModel.navigateTo(ScreenState.LIBRARY)
                            },
                            onRename = { recording, newTitle ->
                                viewModel.renameRecording(recording, newTitle)
                            },
                            onDelete = { recording ->
                                viewModel.deleteRecording(recording)
                            },
                            onEditRecording = { recording ->
                                viewModel.openEditorForRecording(recording)
                            }
                        )
                    } ?: run {
                        viewModel.navigateTo(ScreenState.LIBRARY)
                    }
                }
                ScreenState.EDITOR -> {
                    selectedRecording?.let { rec ->
                        VideoEditorScreen(
                            recording = rec,
                            onBack = {
                                viewModel.navigateTo(ScreenState.PREVIEW)
                            },
                            onSaveEditedVideo = { newEntity ->
                                viewModel.saveEditedRecording(newEntity)
                            }
                        )
                    } ?: run {
                        viewModel.navigateTo(ScreenState.LIBRARY)
                    }
                }
                ScreenState.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onBack = {
                            viewModel.navigateTo(ScreenState.DASHBOARD)
                        },
                        onToggleDarkMode = {
                            viewModel.toggleDarkMode()
                        }
                    )
                }
            }

            // Webcam FaceCam Overlay (Floating on top)
            if (settings.isWebcamEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                DraggableWebcamOverlay(
                    shape = settings.webcamShape,
                    size = settings.webcamSize,
                    onClose = {
                        ScreenRecorderManager.updateSettings { it.copy(isWebcamEnabled = false) }
                    },
                    onShapeChange = { newShape ->
                        ScreenRecorderManager.updateSettings { it.copy(webcamShape = newShape) }
                    }
                )
            }

            // Floating Recording Controls Widget (Images 1 & 7: Draggable on-screen widget)
            if ((recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) && settings.isFloatingControlEnabled) {
                FloatingRecordingWidget(
                    state = recordingState,
                    durationMs = durationMs,
                    isMicActive = settings.isMicEnabled,
                    isWebcamActive = settings.isWebcamEnabled,
                    onPauseResume = {
                        if (recordingState == RecordingState.PAUSED) {
                            viewModel.resumeRecording(context)
                        } else {
                            viewModel.pauseRecording(context)
                        }
                    },
                    onStop = { viewModel.stopRecording(context) },
                    onToggleBrush = { isBrushActive = !isBrushActive },
                    onTakeScreenshot = { viewModel.takeScreenshot(context) },
                    onToggleMic = {
                        ScreenRecorderManager.updateSettings { it.copy(isMicEnabled = !it.isMicEnabled) }
                    },
                    onToggleWebcam = {
                        ScreenRecorderManager.updateSettings { it.copy(isWebcamEnabled = !it.isWebcamEnabled) }
                    }
                )
            }

            // Screen Brush Doodle Canvas (Images 1 & 7)
            if (isBrushActive) {
                ScreenBrushOverlay(
                    onClose = { isBrushActive = false }
                )
            }

            // Video Saved Social Sharing Dialog (Image 2)
            savedVideoForDialog?.let { rec ->
                VideoSavedDialog(
                    recording = rec,
                    onDismiss = { viewModel.dismissSavedVideoDialog() },
                    onPlay = {
                        viewModel.selectRecordingForPreview(rec)
                        viewModel.dismissSavedVideoDialog()
                    },
                    onEdit = {
                        viewModel.openEditorForRecording(rec)
                    }
                )
            }

            // Screenshot Result Dialog (Image 6)
            latestScreenshotPath?.let { path ->
                ScreenshotResultDialog(
                    screenshotPath = path,
                    onDismiss = { viewModel.dismissScreenshotDialog() },
                    onEdit = { isBrushActive = true }
                )
            }

            // Recording Floating HUD Bar (Always accessible bottom control)
            AnimatedVisibility(
                visible = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                RecordingHud(
                    state = recordingState,
                    durationMs = durationMs,
                    amplitude = amplitude,
                    isMicActive = settings.isMicEnabled,
                    onPause = { viewModel.pauseRecording(context) },
                    onResume = { viewModel.resumeRecording(context) },
                    onStop = {
                        viewModel.stopRecording(context)
                    },
                    onCancel = {
                        viewModel.stopRecording(context)
                    }
                )
            }

            // Pre-recording 3-2-1 Countdown Overlay
            if (recordingState == RecordingState.COUNTDOWN && countdownRemaining > 0) {
                CountdownOverlay(
                    secondsRemaining = countdownRemaining,
                    onCancel = {
                        viewModel.cancelCountdown()
                    }
                )
            }
        }
    }
}
