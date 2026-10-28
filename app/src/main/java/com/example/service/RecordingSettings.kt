package com.example.service

enum class CaptureSource(val label: String, val description: String) {
    FULL_SCREEN("Entire Screen", "Record full device display and apps"),
    GAME_CAPTURE("Game & Stream", "High 60FPS mode for games & live streaming")
}

enum class VideoQuality(val label: String, val width: Int, val height: Int, val bitrate: Int, val fps: Int) {
    SD_480P("480P", 854, 480, 2_000_000, 30),
    HD_640P("640P", 960, 640, 3_000_000, 30),
    HD_720P("720P HD", 1280, 720, 4_000_000, 30),
    FHD_1080P("1080P FHD", 1920, 1080, 8_000_000, 60),
    QHD_2K("2K QHD", 2560, 1440, 12_000_000, 60),
    UHD_4K("4K UHD", 3840, 2160, 16_000_000, 60)
}

enum class AudioSourceOption(val label: String) {
    INTERNAL("Internal Audio"),
    MIC("Microphone"),
    INTERNAL_AND_MIC("Internal & Mic Sound"),
    MUTE("Mute Audio")
}

enum class WebcamShape {
    CIRCLE,
    ROUNDED_RECT
}

enum class WebcamSize(val sizeDp: Int) {
    SMALL(100),
    MEDIUM(140),
    LARGE(180)
}

data class RecordingSettings(
    val captureSource: CaptureSource = CaptureSource.FULL_SCREEN,
    val videoQuality: VideoQuality = VideoQuality.HD_720P,
    val audioOption: AudioSourceOption = AudioSourceOption.MIC,
    val resolutionLabel: String = "1080P",
    val qualityBitrateMbps: Int = 16,
    val frameRateFps: Int = 60,
    val videoSegmentSizeGb: Int = 3,
    val isMicEnabled: Boolean = true,
    val isSystemAudioEnabled: Boolean = true,
    val isWebcamEnabled: Boolean = false,
    val webcamShape: WebcamShape = WebcamShape.CIRCLE,
    val webcamSize: WebcamSize = WebcamSize.MEDIUM,
    val countdownSeconds: Int = 3,
    val isDarkMode: Boolean = true,
    val isFloatingControlEnabled: Boolean = true,
    val isBrushEnabled: Boolean = false
)

enum class RecordingState {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPED
}
