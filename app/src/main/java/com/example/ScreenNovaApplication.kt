package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.ads.AdManager
import com.example.service.RecordingState
import com.example.service.ScreenRecorderManager

/**
 * ScreenNova Application class.
 * Handles lifecycle observation for Google AdMob App Open Ads on cold start and foreground resume.
 */
class ScreenNovaApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var numStartedActivities = 0
    private var isAppInForeground = false

    companion object {
        private const val TAG = "ScreenNovaApplication"
        lateinit var instance: ScreenNovaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)

        // Preload and initialize Google Mobile Ads SDK early
        AdManager.initialize(this) {
            Log.d(TAG, "AdMob MobileAds initialized from Application")
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        numStartedActivities++
        if (numStartedActivities == 1 && !isAppInForeground) {
            isAppInForeground = true
            Log.d(TAG, "App entered foreground (Cold or Warm launch)")
            onAppForegrounded(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        numStartedActivities--
        if (numStartedActivities <= 0) {
            numStartedActivities = 0
            isAppInForeground = false
            Log.d(TAG, "App entered background")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    private fun onAppForegrounded(activity: Activity) {
        // Safe check: Never interrupt an active recording with an ad
        val state = ScreenRecorderManager.recordingState.value
        if (state == RecordingState.RECORDING || state == RecordingState.PAUSED || state == RecordingState.COUNTDOWN) {
            Log.d(TAG, "Skipping App Open Ad: Active recording in progress ($state)")
            return
        }

        // Show App Open Ad if available
        AdManager.showAppOpenAd(activity) {
            Log.d(TAG, "App Open Ad flow completed or dismissed")
        }
    }

    fun getCurrentActivity(): Activity? = currentActivity
}
