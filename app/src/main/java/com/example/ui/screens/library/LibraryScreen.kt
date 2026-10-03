package com.example.ui.screens.library

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.data.model.MediaType
import com.example.data.repository.SaveTrickRepository
import com.example.ui.components.ModernAudioPlayerSheet
import com.example.ui.components.ModernPhotoViewerDialog
import com.example.ui.components.ModernVideoPlayerDialog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppAccentColor
import com.example.util.FormatUtils
import com.example.util.MediaShareHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LibraryScreen(
    repository: SaveTrickRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor = LocalAppAccentColor.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Downloading, 1: Downloaded, 2: History

    // Reactive database flows
    val activeDownloads by repository.activeDownloads.collectAsState(initial = emptyList())
    val downloadedItems by repository.downloadedItems.collectAsState(initial = emptyList())
    val historyDownloads by repository.historyDownloads.collectAsState(initial = emptyList())

    // Real Room statistics
    val totalDownloads by repository.totalDownloadsCount.collectAsState(initial = 0)
    val videoCount by repository.videoDownloadsCount.collectAsState(initial = 0)
    val audioCount by repository.audioDownloadsCount.collectAsState(initial = 0)
    val totalStorage by repository.totalStorageUsed.collectAsState(initial = 0L)

    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }

    // In-App Media Viewer States
    var activeVideoItem by remember { mutableStateOf<DownloadEntity?>(null) }
    var activeAudioItem by remember { mutableStateOf<DownloadEntity?>(null) }
    var activePhotoItem by remember { mutableStateOf<DownloadEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Statistics Overview Card (Total, Videos, Audios, Storage)
        StatsCard(
            total = totalDownloads,
            videos = videoCount,
            audios = audioCount,
            storage = FormatUtils.formatBytes(totalStorage),
            accentColor = accentColor
        )

        // Tabs: Downloading | Downloaded | History
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
                            text = "${stringResource(R.string.tab_downloading)} (${activeDownloads.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_downloading")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.tab_downloaded)} (${downloadedItems.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_downloaded")
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.tab_history)} (${historyDownloads.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Downloading tab
                if (activeDownloads.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Movie,
                        message = stringResource(R.string.no_downloading)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(activeDownloads, key = { it.id }) { item ->
                            DownloadingItemCard(
                                item = item,
                                onCancel = { repository.cancelDownload(item.id) },
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }

            1 -> {
                // Downloaded tab
                if (downloadedItems.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Movie,
                        message = stringResource(R.string.no_downloaded)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(downloadedItems, key = { it.id }) { item ->
                            DownloadedItemCard(
                                item = item,
                                onOpen = {
                                    when (item.mediaType.uppercase()) {
                                        "VIDEO" -> activeVideoItem = item
                                        "AUDIO" -> activeAudioItem = item
                                        else -> activePhotoItem = item
                                    }
                                },
                                onShare = {
                                    MediaShareHelper.shareMediaFile(context, item)
                                },
                                onDelete = { itemToDelete = item }
                            )
                        }
                    }
                }
            }

            2 -> {
                // History tab
                if (historyDownloads.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Movie,
                        message = stringResource(R.string.no_history)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyDownloads, key = { it.id }) { item ->
                            HistoryItemCard(
                                item = item,
                                onRetry = {
                                    repository.startDownload(
                                        sourceUrl = item.sourceUrl,
                                        mediaUrl = item.sourceUrl,
                                        title = item.title,
                                        thumbnail = item.thumbnail,
                                        mediaType = when (item.mediaType.uppercase()) {
                                            "VIDEO" -> MediaType.VIDEO
                                            "AUDIO" -> MediaType.AUDIO
                                            else -> MediaType.IMAGE
                                        }
                                    )
                                    Toast.makeText(context, context.getString(R.string.retrying_download), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modern In-App Video Player Dialog
    activeVideoItem?.let { item ->
        ModernVideoPlayerDialog(
            videoUrl = item.filePath,
            title = item.title,
            onDismiss = { activeVideoItem = null },
            onShare = { MediaShareHelper.shareMediaFile(context, item) },
            onOpenExternal = { MediaShareHelper.openMediaFile(context, item) }
        )
    }

    // Modern In-App MP3 Player Sheet
    activeAudioItem?.let { item ->
        ModernAudioPlayerSheet(
            audioUrl = item.filePath,
            title = item.title,
            thumbnailUrl = item.thumbnail,
            onDismiss = { activeAudioItem = null },
            onShare = { MediaShareHelper.shareMediaFile(context, item) }
        )
    }

    // Modern In-App Photo Viewer Dialog
    activePhotoItem?.let { item ->
        ModernPhotoViewerDialog(
            images = listOf(item.filePath),
            initialIndex = 0,
            onDismiss = { activePhotoItem = null },
            onShareSingle = { _, _ -> MediaShareHelper.shareMediaFile(context, item) }
        )
    }

    // Confirm Delete Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.deleteDownloaded(item.id)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_confirm),
                        color = ErrorRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }
}

@Composable
private fun StatsCard(
    total: Int,
    videos: Int,
    audios: Int,
    storage: String,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = stringResource(R.string.stat_downloads), value = total.toString(), color = accentColor)
            StatItem(label = stringResource(R.string.stat_videos), value = videos.toString(), color = accentColor)
            StatItem(label = stringResource(R.string.stat_audios), value = audios.toString(), color = accentColor)
            StatItem(label = stringResource(R.string.stat_storage), value = storage, color = accentColor)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun DownloadingItemCard(
    item: DownloadEntity,
    onCancel: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("downloading_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = item.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = when (item.mediaType) {
                                "VIDEO" -> Icons.Default.Movie
                                "AUDIO" -> Icons.Default.Audiotrack
                                else -> Icons.Default.Image
                            },
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Text(
                        text = "${FormatUtils.formatBytes(item.downloadedBytes)} / ${if (item.totalBytes > 0) FormatUtils.formatBytes(item.totalBytes) else "--"} • ${item.downloadSpeed}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("btn_cancel_download_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = ErrorRed
                    )
                }
            }

            if (item.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun DownloadedItemCard(
    item: DownloadEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("downloaded_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when (item.mediaType) {
                            "VIDEO" -> Icons.Default.Movie
                            "AUDIO" -> Icons.Default.Audiotrack
                            else -> Icons.Default.Image
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = "${FormatUtils.formatBytes(item.fileSize)} • ${FormatUtils.formatShortDate(item.completedAt ?: item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Row {
                IconButton(onClick = onOpen, modifier = Modifier.testTag("btn_open_${item.id}")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.action_open),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.testTag("btn_share_${item.id}")) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.action_share),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_${item.id}")) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: DownloadEntity,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Text(
                    text = "Status: ${item.status} • ${FormatUtils.formatShortDate(item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (item.status == "FAILED") ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.action_retry),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
