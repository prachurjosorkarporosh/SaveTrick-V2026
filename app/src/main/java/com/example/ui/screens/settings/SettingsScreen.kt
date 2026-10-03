package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.SaveTrickRepository
import com.example.ui.components.BrandHeader
import com.example.ui.theme.AvailableAccents
import com.example.ui.theme.LocalAppAccentColor
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: SaveTrickRepository,
    onThemeChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentTheme by repository.themeMode.collectAsState()
    val currentAccent by repository.accentColor.collectAsState()
    val currentBgStyle by repository.bgStyle.collectAsState()
    val currentLang by repository.language.collectAsState()
    val userName by repository.userName.collectAsState()
    val isPro by repository.isPro.collectAsState()
    val accentColor = LocalAppAccentColor.current

    var cacheSize by remember { mutableLongStateOf(repository.getCacheSizeBytes()) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBgStyleDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showAudioBitrateDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var autoPasteEnabled by remember { mutableStateOf(repository.preferences.isAutoPasteFromClipboard()) }
    var autoDownloadShareEnabled by remember { mutableStateOf(repository.preferences.isAutoDownloadOnShare()) }
    var notificationsEnabled by remember { mutableStateOf(repository.preferences.isNotificationsEnabled()) }
    var wifiOnlyEnabled by remember { mutableStateOf(repository.preferences.isWifiOnly()) }
    var hapticEnabled by remember { mutableStateOf(repository.preferences.isHapticEnabled()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BrandHeader(showVersion = true)

        // 1. User Profile Section
        SectionHeader(title = stringResource(R.string.profile_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "SaveTrick User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "UID: ${repository.preferences.getUid()}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SaveTrick UID", repository.preferences.getUid()))
                                    Toast.makeText(context, context.getString(R.string.uid_copied), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy UID",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showEditNameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Visual Themes & Colors Section ("aro onek them edd koro bg them ed koro... aro colur theme add koroo")
        SectionHeader(title = "Appearance & Themes")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color Themes selection row
                Text(
                    text = "Accent Color Theme",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AvailableAccents) { accent ->
                        val isSelected = currentAccent == accent.key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    repository.preferences.setAccentColor(accent.key)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accent.primary)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentLang == "bn") accent.nameBn else accent.nameEn,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accent.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Theme Mode (Light / Dark / AMOLED / System)
                SettingsRowClickable(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme_mode),
                    subtitle = when (currentTheme.lowercase()) {
                        "dark" -> stringResource(R.string.theme_dark)
                        "amoled" -> "AMOLED Pure Black"
                        "light" -> stringResource(R.string.theme_light)
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )

                // Background Style (Clean, Mesh Gradient, Cyber Glow, AMOLED)
                SettingsRowClickable(
                    icon = Icons.Default.Wallpaper,
                    title = "Background Style",
                    subtitle = when (currentBgStyle) {
                        "mesh_gradient" -> "Ambient Mesh Gradient"
                        "cyber_glow" -> "Cyber Glow Dark"
                        "amoled_pitch" -> "AMOLED Pure Black"
                        else -> "Clean Minimal"
                    },
                    onClick = { showBgStyleDialog = true }
                )
            }
        }

        // 3. Automation & Direct Share Section
        SectionHeader(title = "Download Preferences")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Auto-Download on TikTok Share
                SettingsRowSwitch(
                    icon = Icons.Default.Share,
                    title = "Auto-Download on Direct Share",
                    subtitle = "Instantly download when link is shared from TikTok",
                    checked = autoDownloadShareEnabled,
                    onCheckedChange = {
                        autoDownloadShareEnabled = it
                        repository.preferences.setAutoDownloadOnShare(it)
                    }
                )

                // Auto-Paste from Clipboard
                SettingsRowSwitch(
                    icon = Icons.Default.ContentPaste,
                    title = "Auto-Paste TikTok Link",
                    subtitle = "Automatically detect copied TikTok links on launch",
                    checked = autoPasteEnabled,
                    onCheckedChange = {
                        autoPasteEnabled = it
                        repository.preferences.setAutoPasteFromClipboard(it)
                    }
                )

                // Default Video Quality
                SettingsRowClickable(
                    icon = Icons.Default.HighQuality,
                    title = stringResource(R.string.default_resolution),
                    subtitle = repository.preferences.getDefaultResolution(),
                    onClick = { showResolutionDialog = true }
                )

                // Audio Bitrate
                SettingsRowClickable(
                    icon = Icons.Default.Audiotrack,
                    title = "Audio Download Quality",
                    subtitle = repository.preferences.getAudioBitrate(),
                    onClick = { showAudioBitrateDialog = true }
                )

                // Notifications
                SettingsRowSwitch(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.download_notifications),
                    subtitle = "Show progress, download speed, and completion alerts",
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        repository.preferences.setNotificationsEnabled(it)
                    }
                )

                // Wi-Fi Only
                SettingsRowSwitch(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.wifi_only),
                    subtitle = "Save mobile data by downloading only on Wi-Fi",
                    checked = wifiOnlyEnabled,
                    onCheckedChange = {
                        wifiOnlyEnabled = it
                        repository.preferences.setWifiOnly(it)
                    }
                )

                // Haptic Feedback
                SettingsRowSwitch(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Vibration",
                    subtitle = "Vibrate gently on download completion",
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        repository.preferences.setHapticEnabled(it)
                    }
                )

                // Storage Destination
                SettingsRowClickable(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.storage_destination),
                    subtitle = if (repository.preferences.getStorageDestination() == "downloads_public") {
                        stringResource(R.string.storage_public_downloads)
                    } else {
                        stringResource(R.string.storage_internal)
                    },
                    onClick = { showStorageDialog = true }
                )

                // Application Language
                SettingsRowClickable(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.app_language),
                    subtitle = if (currentLang == "bn") stringResource(R.string.lang_bengali) else stringResource(R.string.lang_english),
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        // 4. Data Management & Storage
        SectionHeader(title = stringResource(R.string.data_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Clear Cache
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cleared = repository.clearTemporaryCache()
                            cacheSize = repository.getCacheSizeBytes()
                            Toast.makeText(
                                context,
                                "Cleared ${FormatUtils.formatBytes(cleared)} of cache",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.clear_cache),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Current cache: ${FormatUtils.formatBytes(cacheSize)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // Clear History
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearHistoryDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.clear_history),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Delete cancelled and failed records",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // 5. About SaveTrick Section (No admin portal)
        SectionHeader(title = stringResource(R.string.about_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SaveTrick v2.5.7",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Developed by Prachurjo Sorkar Porosh",
                    style = MaterialTheme.typography.bodySmall.copy(color = accentColor, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prachurjo.pro.bd/"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
                Text(
                    text = "© 2026 SaveTrick. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialog: Edit Name
    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.edit_name)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempName.trim()
                        if (trimmed.isNotBlank()) {
                            repository.preferences.setUserName(trimmed)
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(stringResource(R.string.save_name))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Theme Mode
    if (showThemeDialog) {
        val themeOptions = listOf(
            "light" to stringResource(R.string.theme_light),
            "dark" to stringResource(R.string.theme_dark),
            "amoled" to "AMOLED Pure Black",
            "system" to stringResource(R.string.theme_system)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_mode)) },
            text = {
                Column {
                    themeOptions.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setThemeMode(mode)
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme.lowercase() == mode,
                                onClick = {
                                    repository.preferences.setThemeMode(mode)
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Background Style
    if (showBgStyleDialog) {
        val bgOptions = listOf(
            "default" to "Clean Minimal",
            "mesh_gradient" to "Ambient Mesh Gradient",
            "cyber_glow" to "Cyber Glow Dark",
            "amoled_pitch" to "AMOLED Pure Black"
        )
        AlertDialog(
            onDismissRequest = { showBgStyleDialog = false },
            title = { Text("Background Style") },
            text = {
                Column {
                    bgOptions.forEach { (style, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setBgStyle(style)
                                    showBgStyleDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentBgStyle == style,
                                onClick = {
                                    repository.preferences.setBgStyle(style)
                                    showBgStyleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBgStyleDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Video Resolution
    if (showResolutionDialog) {
        val resolutions = listOf("1080p", "720p", "480p")
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = { Text(stringResource(R.string.default_resolution)) },
            text = {
                Column {
                    resolutions.forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setDefaultResolution(res)
                                    showResolutionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = repository.preferences.getDefaultResolution() == res,
                                onClick = {
                                    repository.preferences.setDefaultResolution(res)
                                    showResolutionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = res, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Audio Bitrate
    if (showAudioBitrateDialog) {
        val bitrates = listOf(
            "320kbps" to "320 kbps (High Quality MP3)",
            "192kbps" to "192 kbps (Standard MP3)",
            "128kbps" to "128 kbps (Compact MP3)"
        )
        AlertDialog(
            onDismissRequest = { showAudioBitrateDialog = false },
            title = { Text("Audio Download Quality") },
            text = {
                Column {
                    bitrates.forEach { (rate, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setAudioBitrate(rate)
                                    showAudioBitrateDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = repository.preferences.getAudioBitrate() == rate,
                                onClick = {
                                    repository.preferences.setAudioBitrate(rate)
                                    showAudioBitrateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAudioBitrateDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Language
    if (showLanguageDialog) {
        val languages = listOf(
            "en" to stringResource(R.string.lang_english),
            "bn" to stringResource(R.string.lang_bengali)
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.app_language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLang == code,
                                onClick = {
                                    repository.preferences.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Storage
    if (showStorageDialog) {
        val storageOptions = listOf(
            "downloads_public" to stringResource(R.string.storage_public_downloads),
            "internal" to stringResource(R.string.storage_internal)
        )
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text(stringResource(R.string.storage_destination)) },
            text = {
                Column {
                    storageOptions.forEach { (dest, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setStorageDestination(dest)
                                    showStorageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = repository.preferences.getStorageDestination() == dest,
                                onClick = {
                                    repository.preferences.setStorageDestination(dest)
                                    showStorageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Dialog: Clear History Confirm
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text("Are you sure you want to delete all failed and cancelled records from history?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.clearHistory()
                            Toast.makeText(context, context.getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
                        }
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.btn_dismiss))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsRowClickable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsRowSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
