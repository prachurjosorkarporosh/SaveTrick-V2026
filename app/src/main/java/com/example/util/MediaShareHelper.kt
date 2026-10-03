package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.DownloadEntity
import java.io.File

/**
 * Robust FileProvider-backed sharing utility for SaveTrick.
 * Enables sharing downloaded TikTok videos, audio clips, and images to any target application
 * (WhatsApp, Telegram, Messenger, Instagram, Drive, Bluetooth, etc.) with strict URI permissions.
 */
object MediaShareHelper {

    private const val FILE_PROVIDER_AUTHORITY = "com.prachurjo.savetrick.app.fileprovider"

    /**
     * Share a downloaded media entity to other installed apps via Android Intent Chooser.
     */
    fun shareMediaFile(context: Context, item: DownloadEntity) {
        try {
            val file = File(item.filePath)
            if (!file.exists() || file.length() == 0L) {
                Toast.makeText(context, "Media file does not exist on disk", Toast.LENGTH_SHORT).show()
                return
            }

            // Generate content:// URI via FileProvider
            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            } catch (providerEx: Exception) {
                // Fallback to stored URI if FileProvider fails
                if (!item.fileUri.isNullOrBlank()) {
                    Uri.parse(item.fileUri)
                } else {
                    Uri.fromFile(file)
                }
            }

            val mime = item.mimeType.ifBlank {
                when (item.mediaType.uppercase()) {
                    "VIDEO" -> "video/mp4"
                    "AUDIO" -> "audio/mpeg"
                    "IMAGE", "PHOTO_SLIDESHOW" -> "image/jpeg"
                    else -> "*/*"
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                putExtra(Intent.EXTRA_TEXT, "${item.title}\nSaved with SaveTrick")
                // ClipData is required on Android 10+ (API 29+) to grant URI read permission to the recipient app
                clipData = ClipData.newRawUri(item.title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant permission to all matching resolve activities
            val resInfoList = context.packageManager.queryIntentActivities(chooser, 0)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share media: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open media file in external viewer/player app.
     */
    fun openMediaFile(context: Context, item: DownloadEntity) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File no longer exists on disk", Toast.LENGTH_SHORT).show()
                return
            }

            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            } catch (_: Exception) {
                Uri.parse(item.fileUri)
            }

            val mime = item.mimeType.ifBlank {
                when (item.mediaType.uppercase()) {
                    "VIDEO" -> "video/mp4"
                    "AUDIO" -> "audio/mpeg"
                    "IMAGE", "PHOTO_SLIDESHOW" -> "image/jpeg"
                    else -> "*/*"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mime)
                clipData = ClipData.newRawUri(item.title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to play this file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
