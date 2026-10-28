package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RecordingEntity
import com.example.service.CaptureSource
import com.example.service.RecordingSettings
import com.example.service.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScreenNova", appName)
    }

    @Test
    fun `default recording settings are configured`() {
        val settings = RecordingSettings()
        assertEquals(CaptureSource.FULL_SCREEN, settings.captureSource)
        assertEquals(VideoQuality.HD_720P, settings.videoQuality)
        assertTrue(settings.isMicEnabled)
    }

    @Test
    fun `recording entity properties`() {
        val entity = RecordingEntity(
            title = "Test Capture",
            filePath = "/storage/test.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080
        )
        assertEquals("Test Capture", entity.title)
        assertEquals(5000L, entity.durationMs)
        assertEquals(1080, entity.height)
    }

    @Test
    fun `admob configuration contains video and app open ad units`() {
        assertEquals("ca-app-pub-9754500287514915/4172815698", com.example.ads.AdsConfig.INTERSTITIAL_AD_UNIT_ID)
        assertEquals("ca-app-pub-9754500287514915/8768420843", com.example.ads.AdsConfig.REWARDED_AD_UNIT_ID)
        assertEquals("ca-app-pub-9754500287514915/1866216554", com.example.ads.AdsConfig.APP_OPEN_AD_UNIT_ID)
        assertTrue(com.example.ads.AdsConfig.ENABLE_ADS)
    }

    @Test
    fun `ad manager state flows are accessible`() {
        org.junit.Assert.assertNotNull(com.example.ads.AdManager.isInterstitialReady.value)
        org.junit.Assert.assertNotNull(com.example.ads.AdManager.isRewardedReady.value)
        org.junit.Assert.assertNotNull(com.example.ads.AdManager.isAppOpenReady.value)
    }

    @Test
    fun `ads watched condition maps to correct recording duration limit`() {
        com.example.service.ScreenRecorderManager.updateMaxAllowedDuration(1)
        assertEquals(60_000L, com.example.service.ScreenRecorderManager.maxAllowedDurationMs.value)

        com.example.service.ScreenRecorderManager.updateMaxAllowedDuration(3)
        assertEquals(180_000L, com.example.service.ScreenRecorderManager.maxAllowedDurationMs.value)

        com.example.service.ScreenRecorderManager.updateMaxAllowedDuration(5)
        assertEquals(300_000L, com.example.service.ScreenRecorderManager.maxAllowedDurationMs.value)

        com.example.service.ScreenRecorderManager.updateMaxAllowedDuration(10)
        assertEquals(0L, com.example.service.ScreenRecorderManager.maxAllowedDurationMs.value)
    }

    @Test
    fun `screen states include studio editor`() {
        val states = ScreenState.values()
        assertTrue(states.contains(ScreenState.EDITOR))
    }

    @Test
    fun `recording settings options are configured`() {
        val settings = RecordingSettings(
            resolutionLabel = "1080P",
            frameRateFps = 60,
            qualityBitrateMbps = 16
        )
        assertEquals("1080P", settings.resolutionLabel)
        assertEquals(60, settings.frameRateFps)
        assertEquals(16, settings.qualityBitrateMbps)
        assertTrue(settings.isFloatingControlEnabled)
    }
}
