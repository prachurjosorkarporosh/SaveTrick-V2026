package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.DownloadEntity
import java.io.File

/**
 * Robust FileProvider-backed Android Share Intent utility for SaveTrick.
 * Enables sharing downloaded TikTok videos, audio clips, and photo albums directly
 * to WhatsApp, Telegram, Instagram, Messenger, Bluetooth, Gmail, Google Drive, etc.
 */
object MediaShareHelper {

    /**
     * Dynamically resolves the FileProvider authority matching the current application ID.
     */
    private fun getAuthority(context: Context): String {
        return "${context.packageName}.fileprovider"
    }

    /**
     * Resolves a sharable content:// URI for a given file with fallback.
     */
    private fun getSharableUri(context: Context, file: File, storedUriString: String? = null): Uri {
        // If file exists on storage, use FileProvider
        if (file.exists() && file.length() > 0L) {
            try {
                return FileProvider.getUriForFile(context, getAuthority(context), file)
            } catch (e: Exception) {
                // Secondary fallback authority
                try {
                    return FileProvider.getUriForFile(context, "com.prachurjo.savetrick.app.fileprovider", file)
                } catch (_: Exception) {}
            }
        }

        // If stored URI is already a content:// URI from MediaStore
        if (!storedUriString.isNullOrBlank() && storedUriString.startsWith("content://")) {
            return Uri.parse(storedUriString)
        }

        return Uri.fromFile(file)
    }

    /**
     * Share a single downloaded media entity via standard Android Intent Chooser.
     */
    fun shareMediaFile(context: Context, item: DownloadEntity) {
        try {
            val file = File(item.filePath)
            val hasValidFile = file.exists() && file.length() > 0L
            val hasValidContentUri = !item.fileUri.isNullOrBlank() && item.fileUri.startsWith("content://")

            if (!hasValidFile && !hasValidContentUri) {
                Toast.makeText(context, "Media file does not exist on disk", Toast.LENGTH_SHORT).show()
                return
            }

            val contentUri = getSharableUri(context, file, item.fileUri)
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
                putExtra(Intent.EXTRA_TEXT, "${item.title}\n\nDownloaded via SaveTrick (No Watermark)")
                // ClipData is required on Android 10+ (API 29+) to grant URI read permission to recipient app
                clipData = ClipData.newRawUri(item.title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant read permission to all candidate handler apps
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
     * Share raw file path directly with explicit MIME type.
     */
    fun shareDirectFile(context: Context, filePath: String, mimeType: String, title: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                return
            }

            val contentUri = getSharableUri(context, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\nSaved with SaveTrick")
                clipData = ClipData.newRawUri(title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, 0)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share multiple downloaded media items simultaneously (e.g. photo slideshow batches).
     */
    fun shareMultipleMediaFiles(context: Context, items: List<DownloadEntity>, title: String = "TikTok Media") {
        try {
            val validUris = arrayListOf<Uri>()
            for (item in items) {
                val file = File(item.filePath)
                if (file.exists() && file.length() > 0L) {
                    val uri = getSharableUri(context, file, item.fileUri)
                    validUris.add(uri)
                }
            }

            if (validUris.isEmpty()) {
                Toast.makeText(context, "No media files available to share", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, validUris)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title - ${validUris.size} photos downloaded with SaveTrick")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share all photos with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            for (uri in validUris) {
                val resInfoList = context.packageManager.queryIntentActivities(chooser, 0)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open media file in external player / editor app.
     */
    fun openMediaFile(context: Context, item: DownloadEntity) {
        try {
            val file = File(item.filePath)
            val hasValidFile = file.exists() && file.length() > 0L
            val hasValidContentUri = !item.fileUri.isNullOrBlank() && item.fileUri.startsWith("content://")

            if (!hasValidFile && !hasValidContentUri) {
                Toast.makeText(context, "File no longer exists on disk", Toast.LENGTH_SHORT).show()
                return
            }

            val contentUri = getSharableUri(context, file, item.fileUri)
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
