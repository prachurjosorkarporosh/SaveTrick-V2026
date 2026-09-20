package com.example.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.SaveTrickDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.MediaType
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val database = SaveTrickDatabase.getDatabase(context)
    private val dao: DownloadDao = database.downloadDao()
    private val notificationHelper = NotificationHelper(context)
    private val preferences = UserPreferences(context)

    private val activeCalls = ConcurrentHashMap<Long, Call>()
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadEvent = MutableStateFlow<String?>(null)
    val downloadEvent: StateFlow<String?> = _downloadEvent.asStateFlow()

    fun enqueueDownload(
        sourceUrl: String,
        mediaUrl: String,
        title: String,
        thumbnail: String,
        mediaType: MediaType,
        onDuplicateFound: ((DownloadEntity) -> Unit)? = null
    ) {
        scope.launch {
            val mediaTypeStr = mediaType.name

            // 1. Duplicate check: if already DOWNLOADING
            val existingActive = dao.findActiveDownload(sourceUrl, mediaTypeStr)
            if (existingActive != null) {
                _downloadEvent.value = "Already downloading"
                onDuplicateFound?.invoke(existingActive)
                return@launch
            }

            // 2. Duplicate check: if already DOWNLOADED
            val existingCompleted = dao.findCompletedDownload(sourceUrl, mediaTypeStr)
            if (existingCompleted != null) {
                val file = File(existingCompleted.filePath)
                if (file.exists() && file.length() > 0) {
                    _downloadEvent.value = "Already downloaded"
                    onDuplicateFound?.invoke(existingCompleted)
                    return@launch
                }
            }

            // Create initial entity in Room immediately
            val safeTitle = title.ifBlank { "SaveTrick_${System.currentTimeMillis()}" }
            val extension = when (mediaType) {
                MediaType.VIDEO -> ".mp4"
                MediaType.AUDIO -> ".mp3"
                MediaType.IMAGE, MediaType.PHOTO_SLIDESHOW -> ".jpg"
            }
            val mimeType = when (mediaType) {
                MediaType.VIDEO -> "video/mp4"
                MediaType.AUDIO -> "audio/mpeg"
                MediaType.IMAGE, MediaType.PHOTO_SLIDESHOW -> "image/jpeg"
            }
            val cleanTitle = safeTitle.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(40)
            val fileName = "SaveTrick_${cleanTitle}_${System.currentTimeMillis()}$extension"

            val targetDir = getTargetDirectory()
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)

            val initialEntity = DownloadEntity(
                sourceUrl = sourceUrl,
                title = safeTitle,
                thumbnail = thumbnail,
                mediaType = mediaTypeStr,
                filePath = targetFile.absolutePath,
                fileName = fileName,
                status = "DOWNLOADING",
                progress = 0,
                downloadedBytes = 0L,
                totalBytes = 0L,
                downloadSpeed = "-- MB/s",
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis(),
                mimeType = mimeType
            )

            val downloadId = dao.insertDownload(initialEntity)
            executeDownload(downloadId, mediaUrl, targetFile, safeTitle, mimeType)
        }
    }

    private fun executeDownload(
        downloadId: Long,
        mediaUrl: String,
        targetFile: File,
        title: String,
        mimeType: String
    ) {
        val job = scope.launch {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            val partFile = File(targetFile.parentFile, "${targetFile.name}.part")

            try {
                val request = Request.Builder()
                    .url(mediaUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SaveTrick/2.5.7) AppleWebKit/537.36")
                    .build()

                val call = client.newCall(request)
                activeCalls[downloadId] = call

                val response = call.execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()
                val isIndeterminate = totalBytes <= 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(partFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var downloadedBytes = 0L

                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeed = "-- MB/s"
                var lastUiUpdateTime = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastCalc += bytesRead

                    val now = System.currentTimeMillis()
                    val speedTimeDiff = now - lastSpeedCalcTime

                    // Update speed every 600ms
                    if (speedTimeDiff >= 600) {
                        val bytesPerSec = (bytesSinceLastCalc * 1000.0) / speedTimeDiff
                        currentSpeed = FormatUtils.formatSpeed(bytesPerSec)
                        bytesSinceLastCalc = 0L
                        lastSpeedCalcTime = now
                    }

                    // Update UI and notification at most every 350ms to prevent overhead
                    if (now - lastUiUpdateTime >= 350) {
                        lastUiUpdateTime = now
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        } else 0

                        dao.updateProgress(
                            id = downloadId,
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speed = currentSpeed,
                            status = "DOWNLOADING"
                        )

                        if (preferences.isNotificationsEnabled()) {
                            notificationHelper.showProgressNotification(
                                downloadId = downloadId,
                                title = title,
                                progress = progress,
                                indeterminate = isIndeterminate,
                                downloadedFormatted = FormatUtils.formatBytes(downloadedBytes),
                                totalFormatted = if (totalBytes > 0) FormatUtils.formatBytes(totalBytes) else "Unknown",
                                speed = currentSpeed
                            )
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                outputStream = null
                inputStream.close()
                inputStream = null

                // SECTION 34: File Verification
                // 1. Close stream (done)
                // 2. Finalize file: rename partFile to targetFile
                if (partFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = partFile.renameTo(targetFile)
                    if (!renamed) {
                        // Fallback copy if rename fails
                        partFile.copyTo(targetFile, overwrite = true)
                        partFile.delete()
                    }
                }

                // 3. Verify file exists
                if (!targetFile.exists()) {
                    throw Exception("File verification failed: Output file missing")
                }
                // 4. Verify readable
                if (!targetFile.canRead()) {
                    throw Exception("File verification failed: File cannot be read")
                }
                // 5 & 6. Record actual file size
                val actualSize = targetFile.length()
                if (actualSize == 0L) {
                    throw Exception("File verification failed: Empty file downloaded")
                }

                // Create Content Uri for sharing/opening
                val contentUri = try {
                    FileProvider.getUriForFile(
                        context,
                        "com.prachurjo.savetrick.app.fileprovider",
                        targetFile
                    ).toString()
                } catch (e: Exception) {
                    Uri.fromFile(targetFile).toString()
                }

                // 7. Room = DOWNLOADED
                dao.markCompleted(
                    id = downloadId,
                    filePath = targetFile.absolutePath,
                    fileUri = contentUri,
                    fileName = targetFile.name,
                    fileSize = actualSize,
                    completedAt = System.currentTimeMillis()
                )

                // 8 & 9. Notification
                if (preferences.isNotificationsEnabled()) {
                    notificationHelper.showCompletionNotification(
                        downloadId = downloadId,
                        title = title,
                        fileName = targetFile.name
                    )
                } else {
                    notificationHelper.dismissNotification(downloadId)
                }

                Log.d("DownloadManager", "Download $downloadId completed: ${targetFile.absolutePath}")

            } catch (e: Exception) {
                val isCancelled = activeCalls[downloadId]?.isCanceled() == true
                if (isCancelled) {
                    Log.d("DownloadManager", "Download $downloadId was cancelled")
                    dao.markCancelled(downloadId)
                } else {
                    Log.e("DownloadManager", "Download $downloadId failed", e)
                    dao.markFailed(downloadId, e.message ?: "Download failed")
                }
                // Clean up partial file
                if (partFile.exists()) {
                    partFile.delete()
                }
                notificationHelper.dismissNotification(downloadId)
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
                activeCalls.remove(downloadId)
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    fun cancelDownload(downloadId: Long) {
        scope.launch {
            activeCalls[downloadId]?.cancel()
            activeJobs[downloadId]?.cancel()
            activeCalls.remove(downloadId)
            activeJobs.remove(downloadId)

            dao.markCancelled(downloadId)
            notificationHelper.dismissNotification(downloadId)

            // Remove any dangling part files
            val entity = dao.getDownloadById(downloadId)
            if (entity != null && entity.filePath.isNotBlank()) {
                val part = File("${entity.filePath}.part")
                if (part.exists()) part.delete()
            }
        }
    }

    suspend fun deleteDownloadedItem(downloadId: Long) {
        val item = dao.getDownloadById(downloadId) ?: return
        if (item.filePath.isNotBlank()) {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteById(downloadId)
    }

    private fun getTargetDirectory(): File {
        val dest = preferences.getStorageDestination()
        return if (dest == "downloads_public") {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "SaveTrick")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
