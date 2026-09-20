package com.example.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        // For Android 13+ (TIRAMISU) per-app language feature
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
            } catch (_: Throwable) {
            }
        }

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        try {
            @Suppress("DEPRECATION")
            context.applicationContext.resources.updateConfiguration(
                configuration,
                context.applicationContext.resources.displayMetrics
            )
        } catch (_: Throwable) {
        }

        return context.createConfigurationContext(configuration)
    }

    fun getLocalizedConfiguration(baseConfig: Configuration, langCode: String): Configuration {
        val locale = Locale(langCode)
        return Configuration(baseConfig).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
}
