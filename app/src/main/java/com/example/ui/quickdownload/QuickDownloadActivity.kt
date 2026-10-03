package com.example.ui.quickdownload

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MainActivity
import com.example.R
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import com.example.data.repository.SaveTrickRepository
import com.example.util.UrlValidator
import kotlinx.coroutines.launch

class QuickDownloadActivity : ComponentActivity() {

    private lateinit var repository: SaveTrickRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SaveTrickRepository(this)
        val rawUrl = extractUrlFromIntent(intent)

        setContent {
            SnapTubeStyleQuickDownloadDialog(
                initialUrl = rawUrl,
                onDismiss = { finish() },
                onOpenFullApp = { targetUrl ->
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (!targetUrl.isNullOrBlank()) {
                            putExtra(Intent.EXTRA_TEXT, targetUrl)
                        }
                    }
                    startActivity(mainIntent)
                    finish()
                },
                onDownloadRequested = { mediaResult, type ->
                    val sourceUrl = mediaResult.sourceUrl.ifBlank { rawUrl.orEmpty() }
                    val title = mediaResult.title
                    val thumbnail = mediaResult.coverUrl ?: mediaResult.images.firstOrNull() ?: ""

                    when (type) {
                        MediaType.VIDEO -> {
                            val vUrl = mediaResult.videoUrl
                            if (!vUrl.isNullOrBlank()) {
                                repository.startDownload(
                                    sourceUrl = sourceUrl,
                                    mediaUrl = vUrl,
                                    title = title,
                                    thumbnail = thumbnail,
                                    mediaType = MediaType.VIDEO
                                )
                                Toast.makeText(this, "⚡ Video download started! Saving to Gallery...", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        MediaType.AUDIO -> {
                            val aUrl = mediaResult.audioUrl
                            if (!aUrl.isNullOrBlank()) {
                                repository.startDownload(
                                    sourceUrl = sourceUrl,
                                    mediaUrl = aUrl,
                                    title = title,
                                    thumbnail = thumbnail,
                                    mediaType = MediaType.AUDIO
                                )
                                Toast.makeText(this, "🎵 Audio download started! Saving to Music...", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        MediaType.PHOTO_SLIDESHOW, MediaType.IMAGE -> {
                            if (mediaResult.images.isNotEmpty()) {
                                mediaResult.images.forEachIndexed { index, imgUrl ->
                                    repository.startDownload(
                                        sourceUrl = sourceUrl,
                                        mediaUrl = imgUrl,
                                        title = "$title - Photo ${index + 1}",
                                        thumbnail = imgUrl,
                                        mediaType = MediaType.PHOTO_SLIDESHOW
                                    )
                                }
                                Toast.makeText(this, "📸 Downloading ${mediaResult.images.size} Photos in HD...", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        else -> {}
                    }
                },
                resolveUrl = { url -> repository.resolveUrl(url) }
            )
        }
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            return if (!text.isNullOrBlank()) UrlValidator.sanitizeUrl(text) else null
        } else if (Intent.ACTION_VIEW == action) {
            val dataUri = intent.data
            return if (dataUri != null) UrlValidator.sanitizeUrl(dataUri.toString()) else null
        }
        return null
    }
}

@Composable
fun SnapTubeStyleQuickDownloadDialog(
    initialUrl: String?,
    onDismiss: () -> Unit,
    onOpenFullApp: (String?) -> Unit,
    onDownloadRequested: (MediaResult, MediaType) -> Unit,
    resolveUrl: suspend (String) -> Result<MediaResult>
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var resolvedResult by remember { mutableStateOf<MediaResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl.orEmpty()) }

    fun doResolve(url: String) {
        if (url.isBlank()) {
            isLoading = false
            errorMessage = "No valid TikTok link detected."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = resolveUrl(url)
            result.onSuccess { data ->
                resolvedResult = data
                isLoading = false
            }.onFailure { err ->
                isLoading = false
                errorMessage = err.message ?: "Failed to fetch TikTok media info. Please try again."
            }
        }
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            doResolve(initialUrl)
        } else {
            isLoading = false
            errorMessage = "No link shared from TikTok."
        }
    }

    // Outer dismissable backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Sheet Card (SnapTube / VidMate style floating sheet over TikTok)
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280)
            ) + fadeIn()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent clicking through sheet */ }
                    )
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF0F1420),
                tonalElevation = 8.dp,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Drag pill indicator
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Top Bar with Brand and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFE2C55), Color(0xFF25F4EE))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SaveTrick Fast Download",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Direct from TikTok • No Watermark",
                                    color = Color(0xFF25F4EE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onOpenFullApp(currentUrl) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open Full App",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        // High-speed loading state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF25F4EE),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Extracting TikTok Media...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bypassing watermark in full 1080p HD",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    } else if (errorMessage != null) {
                        // Error State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage ?: "Could not load video",
                                color = Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { doResolve(currentUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25F4EE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onOpenFullApp(currentUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Open in App", color = Color.White)
                                }
                            }
                        }
                    } else if (resolvedResult != null) {
                        val media = resolvedResult!!

                        // Media Summary Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF161E2E))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val thumb = media.coverUrl ?: media.images.firstOrNull()
                            if (!thumb.isNullOrBlank()) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = "Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.title.ifBlank { "TikTok Media" },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (!media.author.isNullOrBlank()) "@${media.author}" else "TikTok Creator",
                                    color = Color(0xFF25F4EE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SELECT DOWNLOAD FORMAT",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // SnapTube-Style Option 1: Video HD
                        if (!media.videoUrl.isNullOrBlank()) {
                            QuickOptionCard(
                                title = "Video (No Watermark)",
                                subtitle = "Full 1080p HD • MP4 Video",
                                badgeText = "HD 1080P",
                                icon = Icons.Default.Videocam,
                                iconColor = Color(0xFFFE2C55),
                                onClick = { onDownloadRequested(media, MediaType.VIDEO) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // SnapTube-Style Option 2: Photos Slideshow (if available)
                        if (media.images.isNotEmpty()) {
                            QuickOptionCard(
                                title = "Download Photos (${media.images.size} slides)",
                                subtitle = "Batch save all high-res photos",
                                badgeText = "ORIGINAL",
                                icon = Icons.Default.Image,
                                iconColor = Color(0xFF00E676),
                                onClick = { onDownloadRequested(media, MediaType.PHOTO_SLIDESHOW) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // SnapTube-Style Option 3: Audio MP3
                        if (!media.audioUrl.isNullOrBlank()) {
                            QuickOptionCard(
                                title = "Audio (Original Sound)",
                                subtitle = "High fidelity • 320kbps MP3",
                                badgeText = "MP3",
                                icon = Icons.Default.Audiotrack,
                                iconColor = Color(0xFF25F4EE),
                                onClick = { onDownloadRequested(media, MediaType.AUDIO) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("quick_download_${badgeText.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161E2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = iconColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFE2C55)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
