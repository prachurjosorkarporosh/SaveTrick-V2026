package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUrl: String,
    val title: String,
    val thumbnail: String = "",
    val mediaType: String, // "VIDEO", "AUDIO", "IMAGE"
    val filePath: String = "",
    val fileUri: String = "",
    val fileName: String = "",
    val status: String, // "PENDING", "DOWNLOADING", "DOWNLOADED", "FAILED", "CANCELLED"
    val progress: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeed: String = "-- MB/s",
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val duration: Long? = null,
    val fileSize: Long = 0L,
    val mimeType: String = ""
)
