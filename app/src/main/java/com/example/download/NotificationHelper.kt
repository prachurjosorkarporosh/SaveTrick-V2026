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
                "SaveTrick Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress, file size, and transfer speed"
                setShowBadge(false)
                enableVibration(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE,
                "Completed Downloads",
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

        val headerText = if (indeterminate) "Downloading..." else "$progress% • Downloading"
        val detailText = if (indeterminate) {
            "$downloadedFormatted • $speed"
        } else {
            "Speed: $speed • $downloadedFormatted of $totalFormatted"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("$headerText: $title")
            .setContentText(detailText)
            .setSubText(speed)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$detailText\n$title")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showCompletionNotification(downloadId: Long, title: String, fileName: String) {
        val notifyId = (downloadId % Int.MAX_VALUE).toInt()
        // Dismiss the ongoing progress notification
        dismissNotification(downloadId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifyId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle("Download Complete ✓")
            .setContentText("$title ($fileName)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Successfully saved to your library!\nFile: $fileName")
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

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
