package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: SaveTrickRepository,
    onNavigateToAdminLogin: () -> Unit,
    onThemeChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentTheme by repository.themeMode.collectAsState()
    val currentLang by repository.language.collectAsState()
    val userName by repository.userName.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    var startupAnimEnabled by remember { mutableStateOf(repository.preferences.isStartupAnimationEnabled()) }
    var notificationsEnabled by remember { mutableStateOf(repository.preferences.isNotificationsEnabled()) }
    var wifiOnlyEnabled by remember { mutableStateOf(repository.preferences.isWifiOnly()) }

    // Hidden Admin 4-Tap trigger state
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    fun handleAdminSecretTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 2000L) {
            tapCount = 1
        } else {
            tapCount++
        }
        lastTapTime = now

        if (tapCount >= 4) {
            tapCount = 0
            Toast.makeText(context, "Opening Admin Portal...", Toast.LENGTH_SHORT).show()
            onNavigateToAdminLogin()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Section
        SectionHeader(title = stringResource(R.string.profile_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "SaveTrick User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${stringResource(R.string.uid_label)}: ${repository.preferences.getUid()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SaveTrick UID", repository.preferences.getUid()))
                            Toast.makeText(context, context.getString(R.string.uid_copied), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_copy_uid")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UID",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showEditNameDialog = true },
                        modifier = Modifier.testTag("btn_edit_name")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Preferences & Download Settings
        SectionHeader(title = stringResource(R.string.preferences_header))

        // Direct Material3 Theme Switcher Card (Light / System / Dark)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentTheme.equals("dark", ignoreCase = true)) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.theme_mode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = when (currentTheme.lowercase()) {
                                    "dark" -> stringResource(R.string.theme_dark)
                                    "light" -> stringResource(R.string.theme_light)
                                    else -> stringResource(R.string.theme_system)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    // Direct Quick Toggle (Switches between light and dark)
                    Switch(
                        checked = currentTheme.equals("dark", ignoreCase = true),
                        onCheckedChange = { isDark ->
                            val newMode = if (isDark) "dark" else "light"
                            repository.preferences.setThemeMode(newMode)
                            onThemeChanged(newMode)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricBlue
                        ),
                        modifier = Modifier.testTag("theme_quick_switch")
                    )
                }

                // 3-Option Segmented Selector: Light | System | Dark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themeOptions = listOf(
                        Triple("light", stringResource(R.string.theme_light), Icons.Default.LightMode),
                        Triple("system", stringResource(R.string.theme_system), Icons.Default.BrightnessAuto),
                        Triple("dark", stringResource(R.string.theme_dark), Icons.Default.DarkMode)
                    )

                    themeOptions.forEach { (mode, label, icon) ->
                        val isSelected = currentTheme.equals(mode, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    repository.preferences.setThemeMode(mode)
                                    onThemeChanged(mode)
                                }
                                .padding(vertical = 6.dp),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = stringResource(R.string.default_resolution),
                    subtitle = repository.preferences.getDefaultResolution(),
                    onClick = { showResolutionDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.app_language),
                    subtitle = if (currentLang == "bn") stringResource(R.string.lang_bengali) else stringResource(R.string.lang_english),
                    onClick = { showLanguageDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.storage_destination),
                    subtitle = if (repository.preferences.getStorageDestination() == "downloads_public") "Downloads/SaveTrick" else "App Storage",
                    onClick = { showStorageDialog = true }
                )
                SettingsToggleItem(
                    icon = Icons.Default.Animation,
                    title = stringResource(R.string.startup_animations),
                    checked = startupAnimEnabled,
                    onCheckedChange = {
                        startupAnimEnabled = it
                        repository.preferences.setStartupAnimationEnabled(it)
                    }
                )
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.download_notifications),
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        repository.preferences.setNotificationsEnabled(it)
                    }
                )
                SettingsToggleItem(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.wifi_only),
                    checked = wifiOnlyEnabled,
                    onCheckedChange = {
                        wifiOnlyEnabled = it
                        repository.preferences.setWifiOnly(it)
                    }
                )
            }
        }

        // 3. Data & Storage Management
        SectionHeader(title = stringResource(R.string.data_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.clear_cache),
                    subtitle = "Clear temporary files & thumbnail cache",
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            Toast.makeText(context, context.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cache cleanup completed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.clear_history),
                    subtitle = "Remove download logs & history",
                    onClick = {
                        scope.launch {
                            repository.clearHistory()
                            Toast.makeText(context, context.getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 4. About SaveTrick
        SectionHeader(title = stringResource(R.string.about_header))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SaveTrick brand title with 4-tap detector for Admin Portal
                BrandHeader(
                    showVersion = true,
                    onBrandClick = { handleAdminSecretTap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_brand_header_secret_tap")
                )

                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                )

                // Developer & Website info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Developer: ${stringResource(R.string.developer_name)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Website: ${stringResource(R.string.developer_website)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = ElectricBlue),
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://prachurjo.pro.bd/"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        style = MaterialTheme.typography.labelMedium.copy(color = ElectricBlue),
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "SaveTrick respects your privacy. No personal data collected.", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Text(
                        text = stringResource(R.string.terms_of_service),
                        style = MaterialTheme.typography.labelMedium.copy(color = ElectricBlue),
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "For personal backup and offline viewing only.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Text(
                    text = stringResource(R.string.copyright_notice),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var newNameInput by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(text = stringResource(R.string.edit_name)) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            repository.preferences.setUserName(newNameInput.trim())
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text(text = stringResource(R.string.save_name))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(text = stringResource(R.string.btn_dismiss))
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themes = listOf(
            "light" to stringResource(R.string.theme_light),
            "dark" to stringResource(R.string.theme_dark),
            "system" to stringResource(R.string.theme_system)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(text = stringResource(R.string.theme_mode)) },
            text = {
                Column {
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setThemeMode(mode)
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme.equals(mode, ignoreCase = true),
                                onClick = {
                                    repository.preferences.setThemeMode(mode)
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        val langs = listOf(
            "en" to stringResource(R.string.lang_english),
            "bn" to stringResource(R.string.lang_bengali)
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(text = stringResource(R.string.app_language)) },
            text = {
                Column {
                    langs.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setLanguage(code)
                                    showLanguageDialog = false
                                    val msg = if (code == "bn") "ভাষা পরিবর্তন করে বাংলা করা হয়েছে" else "Language changed to English"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLang == code,
                                onClick = {
                                    repository.preferences.setLanguage(code)
                                    showLanguageDialog = false
                                    val msg = if (code == "bn") "ভাষা পরিবর্তন করে বাংলা করা হয়েছে" else "Language changed to English"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Resolution Dialog
    if (showResolutionDialog) {
        val resolutions = listOf("1080p", "720p")
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = { Text(text = stringResource(R.string.default_resolution)) },
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
                                .padding(vertical = 8.dp),
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
                            Text(text = res)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Storage Dialog
    if (showStorageDialog) {
        val options = listOf(
            "downloads_public" to stringResource(R.string.storage_public_downloads),
            "app_internal" to stringResource(R.string.storage_internal)
        )
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text(text = stringResource(R.string.storage_destination)) },
            text = {
                Column {
                    options.forEach { (dest, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.preferences.setStorageDestination(dest)
                                    showStorageDialog = false
                                }
                                .padding(vertical = 8.dp),
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
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricBlue
            )
        )
    }
}
