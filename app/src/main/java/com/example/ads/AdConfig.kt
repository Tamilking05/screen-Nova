package com.example.ads

/**
 * Centralized AdMob Ads Configuration
 * Contains official Google AdMob App ID, production Ad Unit IDs,
 * official Google demo/test Ad Unit IDs, and toggles for test/production mode.
 */
object AdsConfig {
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    // Production / Configured Ad Unit IDs provided by user
    const val BANNER_AD_UNIT_ID = "ca-app-pub-9754500287514915/2582286449"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9754500287514915/4172815698"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-9754500287514915/8768420843"
    const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-9754500287514915/1866216554"
    const val NATIVE_AD_UNIT_ID = "ca-app-pub-9754500287514915/2247690483"

    // Official Google Android Demo / Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247690483"

    var ENABLE_ADS: Boolean = true
    var USE_TEST_ADS: Boolean = false

    val currentBannerAdId: String
        get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID

    val currentInterstitialAdId: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else INTERSTITIAL_AD_UNIT_ID

    val currentRewardedAdId: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else REWARDED_AD_UNIT_ID

    val currentAppOpenAdId: String
        get() = if (USE_TEST_ADS) TEST_APP_OPEN_AD_UNIT_ID else APP_OPEN_AD_UNIT_ID

    val currentNativeAdId: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else NATIVE_AD_UNIT_ID
}

/**
 * Direct reference object
 */
object AdConfig {
    const val BANNER_AD_ID = AdsConfig.BANNER_AD_UNIT_ID
    const val REWARDED_AD_ID = AdsConfig.REWARDED_AD_UNIT_ID
    const val INTERSTITIAL_AD_ID = AdsConfig.INTERSTITIAL_AD_UNIT_ID
    const val APP_OPEN_AD_ID = AdsConfig.APP_OPEN_AD_UNIT_ID
    const val NATIVE_AD_ID = AdsConfig.NATIVE_AD_UNIT_ID

    val BANNER_AD_UNIT_ID get() = AdsConfig.currentBannerAdId
    val INTERSTITIAL_AD_UNIT_ID get() = AdsConfig.currentInterstitialAdId
    val REWARDED_AD_UNIT_ID get() = AdsConfig.currentRewardedAdId
    val APP_OPEN_AD_UNIT_ID get() = AdsConfig.currentAppOpenAdId
    val NATIVE_AD_UNIT_ID get() = AdsConfig.currentNativeAdId
}
