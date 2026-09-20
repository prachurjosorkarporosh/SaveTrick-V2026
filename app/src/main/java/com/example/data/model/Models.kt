package com.example.data.model

enum class MediaType {
    VIDEO,
    PHOTO_SLIDESHOW,
    IMAGE,
    AUDIO
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    CANCELLED,
    PAUSED
}

data class MediaResult(
    val type: MediaType,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val images: List<String> = emptyList(),
    val durationSeconds: Long = 0,
    val sourceUrl: String
)

data class UserRecord(
    val uid: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val appVersion: String = "2.5.7",
    val platform: String = "Android",
    val downloadCount: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val isPro: Boolean = false,
    val proExpiry: Long? = null
)

data class PaymentOrder(
    val id: String,
    val uid: String,
    val name: String,
    val plan: String = "PRO_MONTHLY",
    val provider: String, // bKash, Nagad
    val amount: Double = 150.0,
    val transactionReference: String,
    val status: String = "PENDING", // PENDING, PAID, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)

data class PaymentSettings(
    val bkashNumber: String = "01710000000",
    val nagadNumber: String = "01810000000",
    val bkashEnabled: Boolean = true,
    val nagadEnabled: Boolean = true,
    val monthlyPriceBdt: Int = 150,
    val paymentInstructions: String = "Send Money to the selected mobile number and copy your Transaction ID (TrxID) to confirm payment."
)

data class AuditLog(
    val id: String,
    val adminId: String,
    val action: String,
    val targetUid: String,
    val metadata: String,
    val createdAt: Long = System.currentTimeMillis()
)
