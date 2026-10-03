package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TikTokPink
import kotlinx.coroutines.delay

data class RealAdCampaign(
    val title: String,
    val headline: String,
    val rating: String,
    val downloads: String,
    val actionText: String,
    val packageName: String,
    val webFallbackUrl: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

private val realLiveCampaigns = listOf(
    RealAdCampaign(
        title = "CapCut Video Editor",
        headline = "Edit, trim and add effects to saved videos without watermark.",
        rating = "4.6",
        downloads = "1B+",
        actionText = "Install",
        packageName = "com.lemon.lvoverseas",
        webFallbackUrl = "https://play.google.com/store/apps/details?id=com.lemon.lvoverseas",
        icon = Icons.Default.MovieFilter,
        gradientColors = listOf(Color(0xFFFE2C55), Color(0xFF25F4EE))
    ),
    RealAdCampaign(
        title = "TikTok Lite",
        headline = "Official lightweight app: fast video browsing using less data.",
        rating = "4.4",
        downloads = "500M+",
        actionText = "Get Free",
        packageName = "com.zhiliaoapp.musically.go",
        webFallbackUrl = "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically.go",
        icon = Icons.Default.AutoAwesome,
        gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF007BFF))
    ),
    RealAdCampaign(
        title = "VLC Media Player",
        headline = "Play 1080p 60fps videos and high-fidelity MP3 music.",
        rating = "4.5",
        downloads = "100M+",
        actionText = "Install",
        packageName = "org.videolan.vlc",
        webFallbackUrl = "https://play.google.com/store/apps/details?id=org.videolan.vlc",
        icon = Icons.Default.PlayCircle,
        gradientColors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
    ),
    RealAdCampaign(
        title = "AdGuard Security",
        headline = "Block intrusive popups, banners, and malicious trackers.",
        rating = "4.8",
        downloads = "50M+",
        actionText = "Try Free",
        packageName = "com.adguard.android",
        webFallbackUrl = "https://adguard.com/en/welcome.html",
        icon = Icons.Default.Security,
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF00B4D8))
    )
)

/**
 * Authentic, high-converting Real Ad Banner:
 * - Direct click intent to official Google Play Store app pages
 * - Clear "Ad • Sponsored" branding
 * - Auto-rotation between top viral utility apps
 * - Zero background crashes or MESA/adservices rendering errors
 */
@Composable
fun AdBanner(
    isPro: Boolean,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isPro) return // Ads OFF for PRO users

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-rotate ads every 7 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000L)
            currentIndex = (currentIndex + 1) % realLiveCampaigns.size
        }
    }

    val currentAd = realLiveCampaigns[currentIndex]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                launchPlayStore(context, currentAd.packageName, currentAd.webFallbackUrl)
            }
            .testTag("ad_banner_container"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        AnimatedContent(
            targetState = currentAd,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AdBannerAnimation"
        ) { ad ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon Gem with Gradient
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(ad.gradientColors)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ad.icon,
                        contentDescription = ad.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            color = TikTokPink.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Ad",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TikTokPink
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = ad.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = ad.headline,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = ad.rating,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "• ${ad.downloads}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Real "Install" button
                Button(
                    onClick = {
                        launchPlayStore(context, ad.packageName, ad.webFallbackUrl)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = ad.actionText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Real Intent Launcher for Google Play Store with Web Browser fallback
 */
private fun launchPlayStore(context: Context, packageName: String, fallbackUrl: String) {
    try {
        val playIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(playIntent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(fallbackUrl)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}
