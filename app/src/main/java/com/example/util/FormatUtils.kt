package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun formatSpeed(bytesPerSec: Double): String {
        if (bytesPerSec <= 0) return "-- MB/s"
        val mbps = bytesPerSec / (1024.0 * 1024.0)
        return if (mbps >= 0.1) {
            DecimalFormat("0.0").format(mbps) + " MB/s"
        } else {
            val kbps = bytesPerSec / 1024.0
            DecimalFormat("0.0").format(kbps) + " KB/s"
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
