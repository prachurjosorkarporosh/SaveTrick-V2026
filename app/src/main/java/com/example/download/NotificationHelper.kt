package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress and speed"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE,
                "Download Completed",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for successfully saved media"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    fun showProgressNotification(
        downloadId: Long,
        title: String,
        progress: Int,
        indeterminate: Boolean,
        downloadedFormatted: String,
        totalFormatted: String,
        speed: String
    ) {
        val notifyId = (downloadId % Int.MAX_VALUE).toInt()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifyId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (indeterminate) {
            "$downloadedFormatted • $speed"
        } else {
            "$progress% ($downloadedFormatted / $totalFormatted) • $speed"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.savetrick_logo)
            .setContentTitle("Downloading: $title")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, indeterminate)

        try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showCompletionNotification(downloadId: Long, title: String, fileName: String) {
        val notifyId = (downloadId % Int.MAX_VALUE).toInt()
        // Dismiss the ongoing progress notification
        dismissNotification(downloadId)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifyId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.savetrick_logo)
            .setContentTitle("Download Complete")
            .setContentText("$title ($fileName)")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notifyId + 100000, builder.build())
        } catch (_: SecurityException) {}
    }

    fun dismissNotification(downloadId: Long) {
        val notifyId = (downloadId % Int.MAX_VALUE).toInt()
        try {
            notificationManager.cancel(notifyId)
        } catch (_: Exception) {}
    }

    companion object {
        const val CHANNEL_PROGRESS = "savetrick_download_progress"
        const val CHANNEL_COMPLETE = "savetrick_download_complete"
    }
}
