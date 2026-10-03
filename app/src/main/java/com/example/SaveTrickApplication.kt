package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class SaveTrickApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        initFirebase()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SaveTrickApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                var app: FirebaseApp? = null
                try {
                    app = FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    Log.w("SaveTrickApp", "Default FirebaseApp init notice: ${e.message}")
                }

                if (app == null && FirebaseApp.getApps(this).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:68919931706:android:b2a9e34c9f187a02c3d4e5")
                        .setApiKey("AIzaSyDummyKeyForFirebaseBuildValidation2026")
                        .setProjectId("savetrick-app-2026")
                        .setStorageBucket("savetrick-app-2026.appspot.com")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.i("SaveTrickApp", "Firebase initialized with fallback configuration")
                }
            }
        } catch (e: Exception) {
            Log.e("SaveTrickApp", "Failed to initialize FirebaseApp", e)
        }
    }
}
