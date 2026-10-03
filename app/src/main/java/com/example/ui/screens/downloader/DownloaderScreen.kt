package com.example.ui.screens.downloader

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import com.example.data.repository.SaveTrickRepository
import com.example.ui.components.AdBanner
import com.example.ui.components.BrandHeader
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
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

    var urlInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Single Link, 1: Batch
    var isResolving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mediaResult by remember { mutableStateOf<MediaResult?>(null) }

    fun handleResolve(targetUrl: String = urlInput) {
        val trimmed = targetUrl.trim()
        if (trimmed.isBlank()) {
            errorMessage = context.getString(R.string.error_empty_url)
            mediaResult = null
            return
        }

        // TikTok URL validation
        val sanitized = UrlValidator.sanitizeUrl(trimmed)
        if (!UrlValidator.isTikTokUrl(sanitized)) {
            // Strict Section 1 error message:
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SaveTrick Brand Header (Logo + Name + Version, NO developer info)
        BrandHeader(
            showVersion = true,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // 2. Tab selector: Single Link vs Batch Downloader
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                            tint = MaterialTheme.colorScheme.primary
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
                        focusedBorderColor = ElectricBlue,
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
                                urlInput = text
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
                            // Batch URLs resolution & download
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_download_resolve")
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
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

        // 4. Error Message Banner (if unsupported link or failed resolution)
        if (errorMessage != null) {
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
        mediaResult?.let { result ->
            when (result.type) {
                MediaType.VIDEO -> {
                    // VIDEO result:
                    // - Correct aspect ratio preview
                    // - ExoPlayer initialized only for valid playable video
                    // - Download Video
                    // - Download Audio only when a real audio stream exists
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
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                                )
                            }

                            // ExoPlayer preview for playable video
                            if (!result.videoUrl.isNullOrBlank()) {
                                VideoPlayerView(
                                    videoUrl = result.videoUrl,
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                            mediaType = MediaType.VIDEO,
                                            onDuplicate = {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.already_downloading),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                        Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
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

                            // Download Audio (only when a real audio stream exists)
                            if (!result.audioUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        repository.startDownload(
                                            sourceUrl = result.sourceUrl,
                                            mediaUrl = result.audioUrl,
                                            title = "${result.title} (Audio)",
                                            thumbnail = result.coverUrl.orEmpty(),
                                            mediaType = MediaType.AUDIO,
                                            onDuplicate = {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.already_downloading),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                        Toast.makeText(context, context.getString(R.string.audio_download_started), Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("download_audio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.download_audio),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                MediaType.PHOTO_SLIDESHOW, MediaType.IMAGE -> {
                    // PHOTO_SLIDESHOW:
                    // - NEVER initialize ExoPlayer
                    // - Show all real images vertically
                    // - Preserve aspect ratio
                    // - Individual Download
                    // - Download All (creates separate Room records for every image)
                    SlideshowView(
                        images = result.images,
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
                else -> {}
            }
        }
    }
}
