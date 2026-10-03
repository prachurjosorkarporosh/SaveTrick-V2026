package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.SaveTrickRepository
import com.example.navigation.AppNavGraph
import com.example.ui.theme.SaveTrickTheme
import com.example.util.LocaleHelper
import com.example.util.UrlValidator

class MainActivity : ComponentActivity() {

    private lateinit var repository: SaveTrickRepository

    override fun attachBaseContext(newBase: Context) {
        try {
            val prefs = newBase.getSharedPreferences("savetrick_preferences", Context.MODE_PRIVATE)
            val saved = prefs.getString("key_lang", null)
            val lang = if (!saved.isNullOrBlank()) {
                saved
            } else {
                val sysLang = java.util.Locale.getDefault().language
                if (sysLang.startsWith("bn")) "bn" else "en"
            }
            val localizedContext = LocaleHelper.applyLocale(newBase, lang)
            super.attachBaseContext(localizedContext)
        } catch (_: Throwable) {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            repository = SaveTrickRepository(this)
            LocaleHelper.applyLocale(this, repository.preferences.getLanguage())
        } catch (t: Throwable) {
            repository = SaveTrickRepository(applicationContext)
        }

        // Handle shared TikTok links from Intent (e.g. Share via SaveTrick)
        handleIncomingIntent(intent)

        setContent {
            val currentTheme by repository.themeMode.collectAsState()
            val currentAccent by repository.accentColor.collectAsState()
            val currentBgStyle by repository.bgStyle.collectAsState()
            val currentLanguage by repository.language.collectAsState()

            val systemConfiguration = LocalConfiguration.current

            val localizedConfiguration = remember(systemConfiguration, currentLanguage) {
                LocaleHelper.getLocalizedConfiguration(systemConfiguration, currentLanguage)
            }

            LaunchedEffect(currentLanguage) {
                try {
                    LocaleHelper.applyLocale(this@MainActivity, currentLanguage)
                } catch (_: Throwable) {}
            }

            // Permission launcher for Storage & Android 13+ Notifications
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
                repository.preferences.setNotificationsEnabled(notifGranted)
            }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (neededPermissions.isNotEmpty()) {
                    permissionsLauncher.launch(neededPermissions.toTypedArray())
                }
            }

            key(currentLanguage) {
                CompositionLocalProvider(
                    LocalConfiguration provides localizedConfiguration
                ) {
                    SaveTrickTheme(
                        themeMode = currentTheme,
                        accentKey = currentAccent,
                        bgStyle = currentBgStyle
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                modifier = Modifier.fillMaxSize()
                            ) { _ ->
                                val navController = rememberNavController()
                                AppNavGraph(
                                    navController = navController,
                                    repository = repository,
                                    onThemeChanged = { newTheme ->
                                        repository.preferences.setThemeMode(newTheme)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                if (!sharedText.isNullOrBlank()) {
                    val sanitized = UrlValidator.sanitizeUrl(sharedText)
                    repository.onIncomingSharedUrl(sanitized)
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val dataUri = intent.data
            if (dataUri != null) {
                val sanitized = UrlValidator.sanitizeUrl(dataUri.toString())
                repository.onIncomingSharedUrl(sanitized)
            }
        }
    }
}
