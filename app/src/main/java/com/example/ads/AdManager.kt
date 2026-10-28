package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized AdManager singleton class for Google AdMob SDK.
 * Handles SDK initialization, wrapper methods for loading and presenting
 * Banner, Interstitial, Rewarded, App Open, and Native ads.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Interstitial Ad State
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    // Rewarded Ad State
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false
    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    // App Open Ad State
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadTime: Long = 0L
    private var isAppOpenLoading = false
    private var isShowingAppOpenAd = false
    private val _isAppOpenReady = MutableStateFlow(false)
    val isAppOpenReady: StateFlow<Boolean> = _isAppOpenReady.asStateFlow()

    // Native Ad State
    private var nativeAd: NativeAd? = null
    private var isNativeLoading = false
    private val _isNativeAdReady = MutableStateFlow(false)
    val isNativeAdReady: StateFlow<Boolean> = _isNativeAdReady.asStateFlow()

    // Latest status message for diagnostics
    private val _adStatusMessage = MutableStateFlow<String?>("Ready")
    val adStatusMessage: StateFlow<String?> = _adStatusMessage.asStateFlow()

    private var isInitialized = false

    /**
     * Initializes the Google Mobile Ads SDK once and preloads ad units.
     */
    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (isInitialized) {
            onInitialized?.invoke()
            return
        }

        if (!AdsConfig.ENABLE_ADS) {
            Log.d(TAG, "Ads are disabled by configuration.")
            _adStatusMessage.value = "Ads Disabled in Config"
            onInitialized?.invoke()
            return
        }

        try {
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)
            MobileAds.disableMediationAdapterInitialization(context)
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up MobileAds request configuration: ${e.message}")
        }

        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
            _adStatusMessage.value = "AdMob Initialized"

            // Preload ads
            loadInterstitialAd(context)
            loadRewardedAd(context)
            loadAppOpenAd(context)
            loadNativeAd(context)

            onInitialized?.invoke()
        }
    }

    // =========================================================================
    // BANNER ADS
    // =========================================================================

    /**
     * Factory helper to create a configured Banner AdView.
     */
    fun createBannerAdView(context: Context, adSize: AdSize = AdSize.BANNER): AdView {
        return AdView(context).apply {
            setAdSize(adSize)
            adUnitId = AdsConfig.currentBannerAdId
            if (AdsConfig.ENABLE_ADS) {
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    // =========================================================================
    // INTERSTITIAL ADS (Full-screen Video / Rich Media)
    // =========================================================================

    fun loadInterstitialAd(context: Context) {
        if (!AdsConfig.ENABLE_ADS || interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdsConfig.currentInterstitialAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "InterstitialAd loaded successfully")
                    interstitialAd = ad
                    isInterstitialLoading = false
                    _isInterstitialReady.value = true
                    _adStatusMessage.value = "Interstitial Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "InterstitialAd failed to load (${error.code}): ${error.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                    _isInterstitialReady.value = false
                    _adStatusMessage.value = "Interstitial Error: ${error.message}"
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit) {
        if (!AdsConfig.ENABLE_ADS) {
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd(activity)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            // Load and display immediately so the user always sees the ad
            loadAndShowInterstitialAd(activity, timeoutMs = 3000L, onDismissed = onDismissed)
        }
    }

    /**
     * Loads and immediately shows the Interstitial video ad once loaded,
     * ensuring ads are reliably shown even on first button press.
     */
    fun loadAndShowInterstitialAd(
        activity: Activity,
        timeoutMs: Long = 3000L,
        onDismissed: () -> Unit
    ) {
        if (!AdsConfig.ENABLE_ADS) {
            onDismissed()
            return
        }

        val existingAd = interstitialAd
        if (existingAd != null) {
            showInterstitialAd(activity, onDismissed)
            return
        }

        isInterstitialLoading = true
        _adStatusMessage.value = "Loading Video Ad..."

        var isDone = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isDone) {
                isDone = true
                isInterstitialLoading = false
                Log.d(TAG, "Interstitial load timed out after ${timeoutMs}ms, proceeding with action")
                onDismissed()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            AdsConfig.currentInterstitialAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        interstitialAd = ad
                        isInterstitialLoading = false
                        _isInterstitialReady.value = true
                        _adStatusMessage.value = "Interstitial Ad Ready"
                        showInterstitialAd(activity, onDismissed)
                    } else {
                        interstitialAd = ad
                        _isInterstitialReady.value = true
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        interstitialAd = null
                        isInterstitialLoading = false
                        _isInterstitialReady.value = false
                        _adStatusMessage.value = "Interstitial Error: ${error.message}"
                        onDismissed()
                    }
                }
            }
        )
    }

    // =========================================================================
    // REWARDED VIDEO ADS
    // =========================================================================

    fun loadRewardedAd(context: Context) {
        if (!AdsConfig.ENABLE_ADS || rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdsConfig.currentRewardedAdId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "RewardedAd loaded successfully")
                    rewardedAd = ad
                    isRewardedLoading = false
                    _isRewardedReady.value = true
                    _adStatusMessage.value = "Rewarded Video Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "RewardedAd failed to load (${error.code}): ${error.message}")
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedReady.value = false
                    _adStatusMessage.value = "Rewarded Error: ${error.message}"
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismissed: () -> Unit
    ) {
        if (!AdsConfig.ENABLE_ADS) {
            onDismissed()
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded video ad dismissed")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    loadRewardedAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Rewarded video ad failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    loadRewardedAd(activity)
                    onDismissed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem)
            }
        } else {
            // Load and immediately show with timeout so user gets the reward
            loadAndShowRewardedAd(activity, timeoutMs = 3500L, onUserEarnedReward = onUserEarnedReward, onDismissed = onDismissed)
        }
    }

    /**
     * Loads and immediately shows the Rewarded Video ad once loaded,
     * ensuring video ads are reliably presented with a timeout fallback.
     */
    fun loadAndShowRewardedAd(
        activity: Activity,
        timeoutMs: Long = 3500L,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismissed: () -> Unit
    ) {
        if (!AdsConfig.ENABLE_ADS) {
            onDismissed()
            return
        }

        val existingAd = rewardedAd
        if (existingAd != null) {
            showRewardedAd(activity, onUserEarnedReward, onDismissed)
            return
        }

        isRewardedLoading = true
        _adStatusMessage.value = "Loading Rewarded Video Ad..."

        var isDone = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isDone) {
                isDone = true
                isRewardedLoading = false
                Log.d(TAG, "Rewarded ad load timed out after ${timeoutMs}ms, proceeding with action")
                onDismissed()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            AdsConfig.currentRewardedAdId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        rewardedAd = ad
                        isRewardedLoading = false
                        _isRewardedReady.value = true
                        _adStatusMessage.value = "Rewarded Video Ad Ready"
                        showRewardedAd(activity, onUserEarnedReward, onDismissed)
                    } else {
                        rewardedAd = ad
                        _isRewardedReady.value = true
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        rewardedAd = null
                        isRewardedLoading = false
                        _isRewardedReady.value = false
                        _adStatusMessage.value = "Rewarded Video Error: ${error.message}"
                        onDismissed()
                    }
                }
            }
        )
    }

    // =========================================================================
    // APP OPEN ADS (Shown at Cold Start & Foreground Resume)
    // =========================================================================

    /**
     * Check if App Open Ad was loaded less than 4 hours ago (Google AdMob requirement)
     */
    fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && (System.currentTimeMillis() - appOpenLoadTime < 4 * 3600 * 1000)
    }

    fun loadAppOpenAd(context: Context) {
        if (!AdsConfig.ENABLE_ADS || isAppOpenAdAvailable() || isAppOpenLoading) return
        isAppOpenLoading = true

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdsConfig.currentAppOpenAdId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "AppOpenAd loaded successfully")
                    appOpenAd = ad
                    appOpenLoadTime = System.currentTimeMillis()
                    isAppOpenLoading = false
                    _isAppOpenReady.value = true
                    _adStatusMessage.value = "App Open Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "AppOpenAd failed to load (${error.code}): ${error.message}")
                    appOpenAd = null
                    isAppOpenLoading = false
                    _isAppOpenReady.value = false
                    _adStatusMessage.value = "App Open Error: ${error.message}"
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onDismissed: () -> Unit) {
        if (!AdsConfig.ENABLE_ADS || isShowingAppOpenAd) {
            onDismissed()
            return
        }

        if (isAppOpenAdAvailable()) {
            val ad = appOpenAd
            if (ad != null) {
                isShowingAppOpenAd = true
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "App open ad dismissed")
                        appOpenAd = null
                        isShowingAppOpenAd = false
                        _isAppOpenReady.value = false
                        loadAppOpenAd(activity)
                        onDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "App open ad failed to show: ${adError.message}")
                        appOpenAd = null
                        isShowingAppOpenAd = false
                        _isAppOpenReady.value = false
                        loadAppOpenAd(activity)
                        onDismissed()
                    }
                }
                ad.show(activity)
                return
            }
        }

        // Ad not available or expired, load next and proceed
        loadAppOpenAd(activity)
        onDismissed()
    }

    /**
     * Loads and immediately shows the App Open Ad once loaded with a timeout fallback.
     */
    fun loadAndShowAppOpenAd(
        activity: Activity,
        timeoutMs: Long = 3000L,
        onDismissed: () -> Unit
    ) {
        if (!AdsConfig.ENABLE_ADS || isShowingAppOpenAd) {
            onDismissed()
            return
        }

        if (isAppOpenAdAvailable()) {
            showAppOpenAd(activity, onDismissed)
            return
        }

        isAppOpenLoading = true
        _adStatusMessage.value = "Loading App Open Ad..."

        var isDone = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isDone) {
                isDone = true
                isAppOpenLoading = false
                Log.d(TAG, "App Open Ad load timed out after ${timeoutMs}ms, proceeding")
                onDismissed()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            activity,
            AdsConfig.currentAppOpenAdId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        appOpenAd = ad
                        appOpenLoadTime = System.currentTimeMillis()
                        isAppOpenLoading = false
                        _isAppOpenReady.value = true
                        _adStatusMessage.value = "App Open Ad Ready"
                        showAppOpenAd(activity, onDismissed)
                    } else {
                        appOpenAd = ad
                        appOpenLoadTime = System.currentTimeMillis()
                        _isAppOpenReady.value = true
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isDone) {
                        isDone = true
                        appOpenAd = null
                        isAppOpenLoading = false
                        _isAppOpenReady.value = false
                        _adStatusMessage.value = "App Open Error: ${error.message}"
                        onDismissed()
                    }
                }
            }
        )
    }

    // =========================================================================
    // NATIVE ADS (Google AdMob Native Advanced)
    // =========================================================================

    fun loadNativeAd(
        context: Context,
        onLoaded: ((NativeAd) -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (!AdsConfig.ENABLE_ADS || isNativeLoading) return
        isNativeLoading = true

        val adLoader = AdLoader.Builder(context, AdsConfig.currentNativeAdId)
            .forNativeAd { ad ->
                Log.d(TAG, "NativeAd loaded successfully")
                nativeAd?.destroy()
                nativeAd = ad
                isNativeLoading = false
                _isNativeAdReady.value = true
                _adStatusMessage.value = "Native Ad Ready"
                onLoaded?.invoke(ad)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "NativeAd failed to load (${error.code}): ${error.message}")
                    isNativeLoading = false
                    _isNativeAdReady.value = false
                    _adStatusMessage.value = "Native Error: ${error.message}"
                    onFailed?.invoke(error)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun getLoadedNativeAd(): NativeAd? = nativeAd
}
