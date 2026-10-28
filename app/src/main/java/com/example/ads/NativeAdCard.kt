package com.example.ads

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Modern Jetpack Compose card embedding a Google AdMob Native Ad.
 * Used in Settings and other screens to display seamless native in-feed ads.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentNativeAd by remember { mutableStateOf<NativeAd?>(AdManager.getLoadedNativeAd()) }
    var isLoading by remember { mutableStateOf(currentNativeAd == null) }

    LaunchedEffect(Unit) {
        if (currentNativeAd == null) {
            AdManager.loadNativeAd(
                context = context,
                onLoaded = { ad ->
                    currentNativeAd = ad
                    isLoading = false
                },
                onFailed = {
                    isLoading = false
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Note: AdManager manages native ad instances
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .testTag("native_ad_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SPONSORED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Featured Recommendation",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val ad = currentNativeAd
            if (ad != null) {
                AndroidView(
                    factory = { ctx ->
                        createNativeAdView(ctx, ad)
                    },
                    update = { view ->
                        populateNativeAdView(ad, view)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " Loading recommendations...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            } else {
                // Fallback recommendation card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Featured Apps & Creator Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Curated recommendations for creators and streamers.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun createNativeAdView(context: Context, nativeAd: NativeAd): NativeAdView {
    val nativeAdView = NativeAdView(context)
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Row: Icon + Headline + Body
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 44), dpToPx(context, 44)).apply {
            marginEnd = dpToPx(context, 10)
        }
    }
    topRow.addView(iconView)
    nativeAdView.iconView = iconView

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(context).apply {
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
    }
    textCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(android.graphics.Color.LTGRAY)
        maxLines = 2
    }
    textCol.addView(bodyView)
    nativeAdView.bodyView = bodyView

    topRow.addView(textCol)
    rootLayout.addView(topRow)

    // Call to action button
    val ctaButton = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 42)
        ).apply {
            topMargin = dpToPx(context, 10)
        }
        setBackgroundColor(android.graphics.Color.parseColor("#4F46E5"))
        setTextColor(android.graphics.Color.WHITE)
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    populateNativeAdView(nativeAd, nativeAdView)
    return nativeAdView
}

private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline ?: "Recommended Application"

    val bodyView = nativeAdView.bodyView as? TextView
    if (nativeAd.body != null) {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    } else {
        bodyView?.visibility = View.GONE
    }

    val iconView = nativeAdView.iconView as? ImageView
    if (nativeAd.icon != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val ctaView = nativeAdView.callToActionView as? Button
    if (nativeAd.callToAction != null) {
        ctaView?.text = nativeAd.callToAction
        ctaView?.visibility = View.VISIBLE
    } else {
        ctaView?.text = "Learn More"
        ctaView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}

private fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}

/**
 * Full-screen style Native Ad Card for Settings screen.
 * Displays sponsored badge, advertiser icon & name, large MediaView, body, and prominent CTA button.
 */
@Composable
fun NativeFullScreenAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentNativeAd by remember { mutableStateOf<NativeAd?>(AdManager.getLoadedNativeAd()) }
    var isLoading by remember { mutableStateOf(currentNativeAd == null) }

    LaunchedEffect(Unit) {
        if (currentNativeAd == null) {
            AdManager.loadNativeAd(
                context = context,
                onLoaded = { ad ->
                    currentNativeAd = ad
                    isLoading = false
                },
                onFailed = {
                    isLoading = false
                }
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .testTag("native_fullscreen_ad_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val ad = currentNativeAd
            if (ad != null) {
                AndroidView(
                    factory = { ctx ->
                        createNativeFullScreenAdView(ctx, ad)
                    },
                    update = { view ->
                        populateNativeFullScreenAdView(ad, view)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " Loading recommendation...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            } else {
                // Fallback card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Curated Creator Tools & Apps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Explore top-rated media creation and streaming utilities.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun createNativeFullScreenAdView(context: Context, nativeAd: NativeAd): NativeAdView {
    val nativeAdView = NativeAdView(context)
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Row: Icon + Headline + Advertiser/Rating
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 48)).apply {
            marginEnd = dpToPx(context, 12)
        }
    }
    topRow.addView(iconView)
    nativeAdView.iconView = iconView

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(context).apply {
        textSize = 15f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
    }
    textCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val advertiserView = TextView(context).apply {
        textSize = 12f
        setTextColor(android.graphics.Color.LTGRAY)
    }
    textCol.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView

    topRow.addView(textCol)
    rootLayout.addView(topRow)

    // MediaView (large ad creative: image or video)
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 190)
        ).apply {
            topMargin = dpToPx(context, 10)
            bottomMargin = dpToPx(context, 10)
        }
    }
    rootLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // Body description
    val bodyView = TextView(context).apply {
        textSize = 13f
        setTextColor(android.graphics.Color.LTGRAY)
        maxLines = 3
    }
    rootLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView

    // Call To Action Button (Full width, bold, prominent)
    val ctaButton = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 48)
        ).apply {
            topMargin = dpToPx(context, 12)
        }
        setBackgroundColor(android.graphics.Color.parseColor("#4F46E5"))
        setTextColor(android.graphics.Color.WHITE)
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    populateNativeFullScreenAdView(nativeAd, nativeAdView)
    return nativeAdView
}

private fun populateNativeFullScreenAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline ?: "Sponsored Promotion"

    val bodyView = nativeAdView.bodyView as? TextView
    if (nativeAd.body != null) {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    } else {
        bodyView?.visibility = View.GONE
    }

    val iconView = nativeAdView.iconView as? ImageView
    if (nativeAd.icon != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val advertiserView = nativeAdView.advertiserView as? TextView
    if (nativeAd.advertiser != null) {
        advertiserView?.text = nativeAd.advertiser
        advertiserView?.visibility = View.VISIBLE
    } else {
        advertiserView?.visibility = View.GONE
    }

    val mediaView = nativeAdView.mediaView as? MediaView
    if (nativeAd.mediaContent != null && mediaView != null) {
        mediaView.mediaContent = nativeAd.mediaContent
        mediaView.visibility = View.VISIBLE
    } else {
        mediaView?.visibility = View.GONE
    }

    val ctaView = nativeAdView.callToActionView as? Button
    if (nativeAd.callToAction != null) {
        ctaView?.text = nativeAd.callToAction
        ctaView?.visibility = View.VISIBLE
    } else {
        ctaView?.text = "Install Now"
        ctaView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}
