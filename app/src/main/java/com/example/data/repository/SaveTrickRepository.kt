package com.example.data.repository

import android.content.Context
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.SaveTrickDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import com.example.data.model.PaymentOrder
import com.example.data.model.PaymentSettings
import com.example.data.model.UserRecord
import com.example.data.remote.FirebaseBackendClient
import com.example.data.remote.TikTokResolverService
import com.example.download.DownloadManager
import com.example.util.UrlValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class SaveTrickRepository(private val context: Context) {

    private val database = SaveTrickDatabase.getDatabase(context)
    val downloadDao: DownloadDao = database.downloadDao()
    val preferences = UserPreferences(context)
    val resolverService = TikTokResolverService()
    val downloadManager = DownloadManager.getInstance(context)
    val firebaseClient = FirebaseBackendClient(context)

    // Download flows
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val downloadedItems: Flow<List<DownloadEntity>> = downloadDao.getDownloadedItems()
    val historyDownloads: Flow<List<DownloadEntity>> = downloadDao.getHistoryDownloads()

    // Stats flows
    val totalDownloadsCount: Flow<Int> = downloadDao.getDownloadedCount()
    val videoDownloadsCount: Flow<Int> = downloadDao.getVideoCount()
    val audioDownloadsCount: Flow<Int> = downloadDao.getAudioCount()
    val totalStorageUsed: Flow<Long> = downloadDao.getTotalStorageUsed()

    // Preferences
    val themeMode: StateFlow<String> = preferences.themeFlow
    val language: StateFlow<String> = preferences.langFlow
    val isPro: StateFlow<Boolean> = preferences.isProFlow
    val userName: StateFlow<String> = preferences.userNameFlow

    suspend fun resolveUrl(url: String): Result<MediaResult> {
        val sanitized = UrlValidator.sanitizeUrl(url)
        if (!UrlValidator.isTikTokUrl(sanitized)) {
            return Result.failure(IllegalArgumentException("UNSUPPORTED_URL"))
        }
        return resolverService.resolveTikTokUrl(sanitized)
    }

    fun startDownload(
        sourceUrl: String,
        mediaUrl: String,
        title: String,
        thumbnail: String,
        mediaType: MediaType,
        onDuplicate: ((DownloadEntity) -> Unit)? = null
    ) {
        downloadManager.enqueueDownload(
            sourceUrl = sourceUrl,
            mediaUrl = mediaUrl,
            title = title,
            thumbnail = thumbnail,
            mediaType = mediaType,
            onDuplicateFound = onDuplicate
        )
    }

    fun cancelDownload(id: Long) {
        downloadManager.cancelDownload(id)
    }

    suspend fun deleteDownloaded(id: Long) {
        downloadManager.deleteDownloadedItem(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearFailedAndCancelled()
    }

    suspend fun registerUser(name: String): Result<Boolean> {
        preferences.setUserName(name)
        val uid = preferences.getUid()
        return firebaseClient.registerUser(name = name, uid = uid)
    }

    suspend fun syncProStatus() {
        val uid = preferences.getUid()
        val backendProResult = firebaseClient.getProStatus(uid)
        val backendPro = backendProResult.getOrDefault(preferences.isProUser())
        if (backendPro != preferences.isProUser()) {
            preferences.setProStatus(backendPro)
        }
    }

    suspend fun getPaymentSettings(): PaymentSettings {
        return firebaseClient.getPaymentSettings()
    }

    suspend fun submitPayment(provider: String, trxId: String, amount: Double): Result<String> {
        return firebaseClient.submitPaymentOrder(
            uid = preferences.getUid(),
            name = preferences.getUserName().ifBlank { "SaveTrick User" },
            provider = provider,
            trxId = trxId,
            amount = amount
        )
    }
}
