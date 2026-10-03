package com.example.ui.screens.downloader

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import com.example.data.repository.SaveTrickRepository
import com.example.service.FloatingDownloaderService
import com.example.ui.components.BrandHeader
import com.example.ui.components.ModernAudioPlayerSheet
import com.example.ui.components.ModernPhotoViewerDialog
import com.example.ui.components.ModernVideoPlayerDialog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppAccentColor
import com.example.util.UrlValidator
import kotlinx.coroutines.launch

@Composable
fun DownloaderScreen(
    repository: SaveTrickRepository,
    isPro: Boolean,
    onNavigateToSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val accentColor = LocalAppAccentColor.current

    val incomingSharedUrl by repository.sharedIncomingUrl.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Single Link, 1: Batch
    var isResolving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mediaResult by remember { mutableStateOf<MediaResult?>(null) }

    // Media Viewer States
    var activeVideoPlayer by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeAudioPlayer by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var activePhotoViewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    fun handleResolve(targetUrl: String = urlInput, autoDownload: Boolean = false) {
        val trimmed = targetUrl.trim()
        if (trimmed.isBlank()) {
            errorMessage = context.getString(R.string.error_empty_url)
            mediaResult = null
            return
        }

        val sanitized = UrlValidator.sanitizeUrl(trimmed)
        if (!UrlValidator.isTikTokUrl(sanitized)) {
            errorMessage = context.getString(R.string.error_unsupported_link)
            mediaResult = null
            return
        }

        errorMessage = null
        isResolving = true
        mediaResult = null

        scope.launch {
            val result = repository.resolveUrl(sanitized)
            isResolving = false
            result.onSuccess { resolved ->
                mediaResult = resolved
                errorMessage = null

                if (autoDownload) {
                    when (resolved.type) {
                        MediaType.VIDEO -> {
                            resolved.videoUrl?.let { vUrl ->
                                repository.startDownload(
                                    sourceUrl = resolved.sourceUrl,
                                    mediaUrl = vUrl,
                                    title = resolved.title,
                                    thumbnail = resolved.coverUrl.orEmpty(),
                                    mediaType = MediaType.VIDEO
                                )
                                Toast.makeText(context, "Direct Share: Video download started!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MediaType.PHOTO_SLIDESHOW, MediaType.IMAGE -> {
                            resolved.images.forEachIndexed { index, imgUrl ->
                                repository.startDownload(
                                    sourceUrl = "${resolved.sourceUrl}#img_${index + 1}",
                                    mediaUrl = imgUrl,
                                    title = "${resolved.title}_Image_${index + 1}",
                                    thumbnail = imgUrl,
                                    mediaType = MediaType.IMAGE
                                )
                            }
                            Toast.makeText(context, "Direct Share: Downloading all ${resolved.images.size} photos!", Toast.LENGTH_SHORT).show()
                        }
                        MediaType.AUDIO -> {
                            resolved.audioUrl?.let { aUrl ->
                                repository.startDownload(
                                    sourceUrl = resolved.sourceUrl,
                                    mediaUrl = aUrl,
                                    title = "${resolved.title} (Audio)",
                                    thumbnail = resolved.coverUrl.orEmpty(),
                                    mediaType = MediaType.AUDIO
                                )
                                Toast.makeText(context, "Direct Share: Audio download started!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }.onFailure { err ->
                if (err.message == "UNSUPPORTED_URL") {
                    errorMessage = context.getString(R.string.error_unsupported_link)
                } else {
                    errorMessage = context.getString(R.string.error_resolution_failed)
                }
                mediaResult = null
            }
        }
    }

    // Auto-detect and resolve TikTok link received via Android Share Sheet
    LaunchedEffect(incomingSharedUrl) {
        val shared = incomingSharedUrl
        if (!shared.isNullOrBlank()) {
            val clean = UrlValidator.sanitizeUrl(shared)
            urlInput = clean
            selectedTab = 0
            repository.clearIncomingSharedUrl()
            val shouldAutoDownload = repository.preferences.isAutoDownloadOnShare()
            handleResolve(clean, autoDownload = shouldAutoDownload)
        }
    }

    // Auto-detect clipboard on first screen open
    LaunchedEffect(Unit) {
        if (urlInput.isBlank() && repository.preferences.isAutoPasteFromClipboard()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString().orEmpty()
                if (UrlValidator.isTikTokUrl(clipText)) {
                    val sanitized = UrlValidator.sanitizeUrl(clipText)
                    urlInput = sanitized
                    Toast.makeText(context, "Auto-pasted TikTok link from clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SaveTrick Brand Header
        BrandHeader(showVersion = true)

        // Floating Popup Window Quick Action Banner ("onnono epe thekeo eta popup kore use kora jabe app ta")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            shadowElevation = 2.dp
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
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Floating Pop-up Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Download while browsing TikTok & other apps",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            showOverlayPermissionDialog = true
                        } else {
                            val serviceIntent = Intent(context, FloatingDownloaderService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            Toast.makeText(context, "SaveTrick Floating Pop-up activated!", Toast.LENGTH_SHORT).show()
                            (context as? Activity)?.moveTaskToBack(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Pop-up",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 3D Visual Hero Banner with Glossy Neon Effect
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_3d_hero_banner")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_home_banner_3d),
                    contentDescription = "SaveTrick 3D Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xCC060B12),
                                    Color(0x66060B12),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "ULTRA HD • NO WATERMARK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Save Any TikTok Media",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 19.sp
                        )
                    )
                    Text(
                        text = "Direct Share, Floating Pop-up, HD Video & MP3",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ElectricCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // 2. Tab selector: Single Link vs Batch Downloader
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_single_link),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_single_link")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_batch),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_batch")
                )
            }
        }

        // 3. URL Input Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.url_input_hint) else stringResource(R.string.batch_url_input_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.btn_clear)
                                )
                            }
                        }
                    },
                    singleLine = selectedTab == 0,
                    maxLines = if (selectedTab == 0) 1 else 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field")
                )

                // Paste & Clear buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString().orEmpty()
                                urlInput = UrlValidator.sanitizeUrl(text)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_paste")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.btn_paste))
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = ""
                            errorMessage = null
                            mediaResult = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.btn_clear))
                    }
                }

                // Download / Resolve Button
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            handleResolve()
                        } else {
                            val urls = urlInput.lines().map { it.trim() }.filter { it.isNotBlank() }
                            if (urls.isEmpty()) {
                                errorMessage = context.getString(R.string.error_empty_url)
                            } else {
                                urls.forEach { u ->
                                    handleResolve(u)
                                }
                            }
                        }
                    },
                    enabled = !isResolving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_download_resolve")
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.resolving),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_resolve),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 4. Error Message Banner
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_banner"),
                color = ErrorRed.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        // 5. Resolved Media Result
        AnimatedVisibility(
            visible = mediaResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                mediaResult?.let { result ->
                    when (result.type) {
                        MediaType.VIDEO -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("video_result_card"),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shadowElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2
                                    )

                                    result.author?.let { author ->
                                        Text(
                                            text = "@$author",
                                            style = MaterialTheme.typography.bodySmall.copy(color = accentColor)
                                        )
                                    }

                                    // Video Player Preview Box with Fullscreen trigger
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (!result.videoUrl.isNullOrBlank()) {
                                            VideoPlayerView(
                                                videoUrl = result.videoUrl,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            IconButton(
                                                onClick = {
                                                    activeVideoPlayer = Pair(result.videoUrl, result.title)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fullscreen,
                                                    contentDescription = "Fullscreen",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Download Video Button
                                    Button(
                                        onClick = {
                                            result.videoUrl?.let { vUrl ->
                                                repository.startDownload(
                                                    sourceUrl = result.sourceUrl,
                                                    mediaUrl = vUrl,
                                                    title = result.title,
                                                    thumbnail = result.coverUrl.orEmpty(),
                                                    mediaType = MediaType.VIDEO
                                                )
                                                Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("download_video_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.download_video),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // Download Audio Button & In-App MP3 Player trigger
                                    if (!result.audioUrl.isNullOrBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    activeAudioPlayer = Triple(result.audioUrl, result.title, result.coverUrl)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Audio",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Play MP3")
                                            }

                                            Button(
                                                onClick = {
                                                    repository.startDownload(
                                                        sourceUrl = result.sourceUrl,
                                                        mediaUrl = result.audioUrl,
                                                        title = "${result.title} (Audio)",
                                                        thumbnail = result.coverUrl.orEmpty(),
                                                        mediaType = MediaType.AUDIO
                                                    )
                                                    Toast.makeText(context, context.getString(R.string.audio_download_started), Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Audiotrack,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = stringResource(R.string.download_audio))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MediaType.PHOTO_SLIDESHOW, MediaType.IMAGE -> {
                            SlideshowView(
                                images = result.images,
                                audioUrl = result.audioUrl,
                                onImageClick = { idx ->
                                    activePhotoViewer = Pair(result.images, idx)
                                },
                                onDownloadAudio = if (!result.audioUrl.isNullOrBlank()) {
                                    {
                                        repository.startDownload(
                                            sourceUrl = result.sourceUrl,
                                            mediaUrl = result.audioUrl,
                                            title = "${result.title} (Audio)",
                                            thumbnail = result.coverUrl.orEmpty(),
                                            mediaType = MediaType.AUDIO
                                        )
                                        Toast.makeText(context, context.getString(R.string.audio_download_started), Toast.LENGTH_SHORT).show()
                                    }
                                } else null,
                                onDownloadSingleImage = { imgUrl, idx ->
                                    repository.startDownload(
                                        sourceUrl = "${result.sourceUrl}#img_$idx",
                                        mediaUrl = imgUrl,
                                        title = "${result.title}_Image_$idx",
                                        thumbnail = imgUrl,
                                        mediaType = MediaType.IMAGE
                                    )
                                    Toast.makeText(context, context.getString(R.string.image_downloading, idx), Toast.LENGTH_SHORT).show()
                                },
                                onDownloadAllImages = {
                                    result.images.forEachIndexed { index, imgUrl ->
                                        repository.startDownload(
                                            sourceUrl = "${result.sourceUrl}#img_${index + 1}",
                                            mediaUrl = imgUrl,
                                            title = "${result.title}_Image_${index + 1}",
                                            thumbnail = imgUrl,
                                            mediaType = MediaType.IMAGE
                                        )
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.queued_images_download, result.images.size),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }

                        MediaType.AUDIO -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Button(
                                        onClick = {
                                            result.audioUrl?.let { aUrl ->
                                                repository.startDownload(
                                                    sourceUrl = result.sourceUrl,
                                                    mediaUrl = aUrl,
                                                    title = result.title,
                                                    thumbnail = result.coverUrl.orEmpty(),
                                                    mediaType = MediaType.AUDIO
                                                )
                                                Toast.makeText(context, context.getString(R.string.audio_download_started), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.download_audio))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty state tutorial guide
        if (mediaResult == null && !isResolving) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.empty_home_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.empty_home_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modern Video Player Fullscreen Dialog
    activeVideoPlayer?.let { (vUrl, title) ->
        ModernVideoPlayerDialog(
            videoUrl = vUrl,
            title = title,
            onDismiss = { activeVideoPlayer = null }
        )
    }

    // Modern MP3 Audio Player Bottom Sheet
    activeAudioPlayer?.let { (aUrl, title, thumb) ->
        ModernAudioPlayerSheet(
            audioUrl = aUrl,
            title = title,
            thumbnailUrl = thumb,
            onDismiss = { activeAudioPlayer = null }
        )
    }

    // Modern Photo Viewer Fullscreen Dialog
    activePhotoViewer?.let { (imgs, idx) ->
        ModernPhotoViewerDialog(
            images = imgs,
            initialIndex = idx,
            onDismiss = { activePhotoViewer = null },
            onDownloadSingle = { url, index ->
                repository.startDownload(
                    sourceUrl = "${url}#img_$index",
                    mediaUrl = url,
                    title = "SaveTrick_Photo_$index",
                    thumbnail = url,
                    mediaType = MediaType.IMAGE
                )
                Toast.makeText(context, "Downloaded Photo #$index", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Overlay Permission Request Dialog
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Enable Floating Pop-up") },
            text = {
                Text("To use SaveTrick over TikTok and other apps, allow 'Display over other apps' in your device settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
