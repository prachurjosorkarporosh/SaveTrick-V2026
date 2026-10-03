package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("savetrick_preferences", Context.MODE_PRIVATE)

    private val _userNameFlow = MutableStateFlow(getUserName())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(getThemeMode())
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _accentColorFlow = MutableStateFlow(getAccentColor())
    val accentColorFlow: StateFlow<String> = _accentColorFlow.asStateFlow()

    private val _bgStyleFlow = MutableStateFlow(getBgStyle())
    val bgStyleFlow: StateFlow<String> = _bgStyleFlow.asStateFlow()

    private val _langFlow = MutableStateFlow(getLanguage())
    val langFlow: StateFlow<String> = _langFlow.asStateFlow()

    private val _isProFlow = MutableStateFlow(isProUser())
    val isProFlow: StateFlow<Boolean> = _isProFlow.asStateFlow()

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getUid(): String {
        var uid = prefs.getString(KEY_UID, null)
        if (uid.isNullOrBlank()) {
            uid = generateSaveTrickUid()
            prefs.edit().putString(KEY_UID, uid).apply()
        }
        return uid
    }

    fun getUserName(): String = prefs.getString(KEY_NAME, "") ?: ""

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
        _userNameFlow.value = name
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeFlow.value = mode
    }

    fun getAccentColor(): String = prefs.getString(KEY_ACCENT_COLOR, "electric_blue") ?: "electric_blue"

    fun setAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
        _accentColorFlow.value = color
    }

    fun getBgStyle(): String = prefs.getString(KEY_BG_STYLE, "default") ?: "default"

    fun setBgStyle(style: String) {
        prefs.edit().putString(KEY_BG_STYLE, style).apply()
        _bgStyleFlow.value = style
    }

    fun getLanguage(): String {
        val saved = prefs.getString(KEY_LANG, null)
        if (!saved.isNullOrBlank()) return saved
        val sysLang = java.util.Locale.getDefault().language
        return if (sysLang.startsWith("bn")) "bn" else "en"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _langFlow.value = lang
    }

    fun getDefaultResolution(): String = prefs.getString(KEY_RESOLUTION, "1080p") ?: "1080p"

    fun setDefaultResolution(res: String) {
        prefs.edit().putString(KEY_RESOLUTION, res).apply()
    }

    fun getAudioBitrate(): String = prefs.getString(KEY_AUDIO_BITRATE, "320kbps") ?: "320kbps"

    fun setAudioBitrate(bitrate: String) {
        prefs.edit().putString(KEY_AUDIO_BITRATE, bitrate).apply()
    }

    fun getStorageDestination(): String = prefs.getString(KEY_STORAGE_DEST, "downloads_public") ?: "downloads_public"

    fun setStorageDestination(dest: String) {
        prefs.edit().putString(KEY_STORAGE_DEST, dest).apply()
    }

    fun isAutoPasteFromClipboard(): Boolean = prefs.getBoolean(KEY_AUTO_PASTE, true)

    fun setAutoPasteFromClipboard(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PASTE, enabled).apply()
    }

    fun isAutoDownloadOnShare(): Boolean = prefs.getBoolean(KEY_AUTO_DOWNLOAD_SHARE, true)

    fun setAutoDownloadOnShare(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD_SHARE, enabled).apply()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC, true)

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun isStartupAnimationEnabled(): Boolean = prefs.getBoolean(KEY_STARTUP_ANIM, true)

    fun setStartupAnimationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STARTUP_ANIM, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun isWifiOnly(): Boolean = prefs.getBoolean(KEY_WIFI_ONLY, false)

    fun setWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
    }

    fun isProUser(): Boolean {
        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        val expiry = prefs.getLong(KEY_PRO_EXPIRY, 0L)
        if (isPro && expiry > 0 && System.currentTimeMillis() > expiry) {
            setProStatus(false, null)
            return false
        }
        return isPro
    }

    fun setProStatus(isPro: Boolean, expiryTimestamp: Long? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_PRO, isPro)
            .putLong(KEY_PRO_EXPIRY, expiryTimestamp ?: 0L)
            .apply()
        _isProFlow.value = isPro
    }

    fun getCreatedAt(): Long {
        val time = prefs.getLong(KEY_CREATED_AT, 0L)
        if (time == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_CREATED_AT, now).apply()
            return now
        }
        return time
    }

    companion object {
        private const val KEY_FIRST_LAUNCH = "key_first_launch"
        private const val KEY_UID = "key_uid"
        private const val KEY_NAME = "key_name"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
        private const val KEY_BG_STYLE = "key_bg_style"
        private const val KEY_LANG = "key_lang"
        private const val KEY_RESOLUTION = "key_resolution"
        private const val KEY_AUDIO_BITRATE = "key_audio_bitrate"
        private const val KEY_STORAGE_DEST = "key_storage_dest"
        private const val KEY_AUTO_PASTE = "key_auto_paste"
        private const val KEY_AUTO_DOWNLOAD_SHARE = "key_auto_download_share"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_STARTUP_ANIM = "key_startup_anim"
        private const val KEY_NOTIFICATIONS = "key_notifications"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_IS_PRO = "key_is_pro"
        private const val KEY_PRO_EXPIRY = "key_pro_expiry"
        private const val KEY_CREATED_AT = "key_created_at"

        fun generateSaveTrickUid(): String {
            val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            val random = SecureRandom()
            val sb = StringBuilder("ST-")
            for (i in 0 until 8) {
                sb.append(alphabet[random.nextInt(alphabet.length)])
            }
            return sb.toString()
        }
    }
}
