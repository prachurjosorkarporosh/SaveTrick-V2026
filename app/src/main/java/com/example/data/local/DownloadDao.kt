package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADED' ORDER BY completedAt DESC, createdAt DESC")
    fun getDownloadedItems(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADED', 'FAILED', 'CANCELLED') ORDER BY createdAt DESC")
    fun getHistoryDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE sourceUrl = :sourceUrl AND mediaType = :mediaType AND status IN ('PENDING', 'DOWNLOADING') LIMIT 1")
    suspend fun findActiveDownload(sourceUrl: String, mediaType: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE sourceUrl = :sourceUrl AND mediaType = :mediaType AND status = 'DOWNLOADED' LIMIT 1")
    suspend fun findCompletedDownload(sourceUrl: String, mediaType: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, downloadSpeed = :speed, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long, speed: String, status: String)

    @Query("UPDATE downloads SET status = 'DOWNLOADED', filePath = :filePath, fileUri = :fileUri, fileName = :fileName, fileSize = :fileSize, completedAt = :completedAt, progress = 100 WHERE id = :id")
    suspend fun markCompleted(id: Long, filePath: String, fileUri: String, fileName: String, fileSize: Long, completedAt: Long)

    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: Long, errorMessage: String)

    @Query("UPDATE downloads SET status = 'CANCELLED' WHERE id = :id")
    suspend fun markCancelled(id: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('FAILED', 'CANCELLED')")
    suspend fun clearFailedAndCancelled()

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADED'")
    fun getDownloadedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADED' AND mediaType = 'VIDEO'")
    fun getVideoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADED' AND mediaType = 'AUDIO'")
    fun getAudioCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM downloads WHERE status = 'DOWNLOADED'")
    fun getTotalStorageUsed(): Flow<Long>
}
