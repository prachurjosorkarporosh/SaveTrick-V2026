package com.example

import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.SaveTrickRepository
import com.example.navigation.AppNavGraph
import com.example.ui.theme.SaveTrickTheme
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private lateinit var repository: SaveTrickRepository

    override fun attachBaseContext(newBase: Context) {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SaveTrickRepository(this)
        LocaleHelper.applyLocale(this, repository.preferences.getLanguage())

        setContent {
            val themeModeFromRepo by repository.themeMode.collectAsState()
            var currentTheme by remember { mutableStateOf(themeModeFromRepo) }
            val currentLanguage by repository.language.collectAsState()

            val systemConfiguration = LocalConfiguration.current
            val baseContext = LocalContext.current

            val localizedConfiguration = remember(systemConfiguration, currentLanguage) {
                LocaleHelper.getLocalizedConfiguration(systemConfiguration, currentLanguage)
            }

            val localizedContext = remember(baseContext, currentLanguage) {
                baseContext.createConfigurationContext(localizedConfiguration)
            }

            LaunchedEffect(currentLanguage) {
                LocaleHelper.applyLocale(this@MainActivity, currentLanguage)
            }

            // Permission launcher for Android 13+ Notifications
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                repository.preferences.setNotificationsEnabled(isGranted)
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            key(currentLanguage) {
                CompositionLocalProvider(
                    LocalConfiguration provides localizedConfiguration,
                    LocalContext provides localizedContext
                ) {
                    SaveTrickTheme(themeMode = currentTheme) {
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
                                        currentTheme = newTheme
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

