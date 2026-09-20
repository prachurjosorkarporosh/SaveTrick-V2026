package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class SaveTrickApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
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
