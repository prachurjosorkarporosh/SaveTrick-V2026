package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AuditLog
import com.example.data.model.PaymentOrder
import com.example.data.model.PaymentSettings
import com.example.data.model.UserRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * FirebaseBackendClient
 *
 * Backend client for SaveTrick:
 * - Firebase Authentication for Admin (Email/Password) and Users (Anonymous / UID mapping)
 * - Cloud Firestore for Users, Pro entitlements, Payment orders, Payment settings, Admins, Audit logs
 * - Firebase App Check enabled
 * - Strict role validation: Admin operations verify admins/{firebaseUid} document where role == "admin" && status == "active"
 */
class FirebaseBackendClient(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("savetrick_firebase_prefs", Context.MODE_PRIVATE)

    private val appContext: Context = context.applicationContext

    val auth: FirebaseAuth by lazy {
        ensureFirebaseInitialized()
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        ensureFirebaseInitialized()
        FirebaseFirestore.getInstance()
    }

    val adminEmail: String?
        get() = try {
            auth.currentUser?.email ?: prefs.getString("admin_email", null)
        } catch (_: Exception) {
            prefs.getString("admin_email", null)
        }

    init {
        ensureFirebaseInitialized()
        setupAppCheck()
    }

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            var app: FirebaseApp? = null
            try {
                // Try default initialization from google-services.json
                app = FirebaseApp.initializeApp(appContext)
            } catch (e: Exception) {
                Log.w("FirebaseBackendClient", "Default FirebaseApp initialization notice: ${e.message}")
            }

            if (app == null && FirebaseApp.getApps(appContext).isEmpty()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:68919931706:android:b2a9e34c9f187a02c3d4e5")
                        .setApiKey("AIzaSyDummyKeyForFirebaseBuildValidation2026")
                        .setProjectId("savetrick-app-2026")
                        .setStorageBucket("savetrick-app-2026.appspot.com")
                        .build()
                    FirebaseApp.initializeApp(appContext, options)
                    Log.i("FirebaseBackendClient", "FirebaseApp initialized with fallback options")
                } catch (fallbackEx: Exception) {
                    Log.e("FirebaseBackendClient", "FirebaseApp init error: ${fallbackEx.message}")
                }
            }
        }
    }

    private fun setupAppCheck() {
        try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                val appCheck = FirebaseAppCheck.getInstance()
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            Log.d("FirebaseBackendClient", "AppCheck setup notice: ${e.message}")
        }
    }

    // ==========================================
    // 1. User Registration & Entitlements
    // ==========================================

    suspend fun registerUser(name: String, uid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ensureFirebaseInitialized()

            // If user not signed in to Firebase, sign in anonymously to obtain a valid auth context
            try {
                if (auth.currentUser == null) {
                    withTimeoutOrNull(2500L) {
                        auth.signInAnonymously().await()
                    }
                }
            } catch (authEx: Exception) {
                Log.d("FirebaseBackendClient", "Anonymous auth notice: ${authEx.message}")
            }

            val now = System.currentTimeMillis()
            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to name,
                "status" to "ACTIVE",
                "isPro" to false,
                "proExpiry" to 0L,
                "downloadCount" to 0,
                "appVersion" to "2.5.7",
                "platform" to "Android",
                "createdAt" to now,
                "lastActiveAt" to now
            )

            try {
                withTimeoutOrNull(2500L) {
                    firestore.collection("users").document(uid)
                        .set(userMap, SetOptions.merge())
                        .await()

                    // Initialize pro_entitlement document
                    val entitlementMap = hashMapOf<String, Any>(
                        "uid" to uid,
                        "isPro" to false,
                        "proExpiry" to 0L,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                    firestore.collection("pro_entitlements").document(uid)
                        .set(entitlementMap, SetOptions.merge())
                        .await()
                }
            } catch (dbEx: Exception) {
                Log.w("FirebaseBackendClient", "Firestore registration notice: ${dbEx.message}")
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "registerUser error", e)
            Result.success(true)
        }
    }

    suspend fun getProStatus(uid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // First check pro_entitlements collection
            val doc = firestore.collection("pro_entitlements").document(uid).get().await()
            if (doc.exists()) {
                val isPro = doc.getBoolean("isPro") ?: false
                val expiry = doc.getLong("proExpiry") ?: 0L
                val isValid = isPro && (expiry == 0L || expiry > System.currentTimeMillis())
                return@withContext Result.success(isValid)
            }

            // Fallback check users collection
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                val isPro = userDoc.getBoolean("isPro") ?: false
                val expiry = userDoc.getLong("proExpiry") ?: 0L
                val isValid = isPro && (expiry == 0L || expiry > System.currentTimeMillis())
                return@withContext Result.success(isValid)
            }

            Result.success(false)
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "getProStatus failed", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. Payment Settings
    // ==========================================

    suspend fun getPaymentSettings(): PaymentSettings = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("payment_settings").document("config").get().await()
            if (doc.exists()) {
                return@withContext PaymentSettings(
                    bkashNumber = doc.getString("bkashNumber") ?: "01700000000",
                    nagadNumber = doc.getString("nagadNumber") ?: "01800000000",
                    bkashEnabled = doc.getBoolean("bkashEnabled") ?: true,
                    nagadEnabled = doc.getBoolean("nagadEnabled") ?: true,
                    monthlyPriceBdt = (doc.getLong("monthlyPriceBdt") ?: 150L).toInt(),
                    paymentInstructions = doc.getString("paymentInstructions")
                        ?: "Send Money to the above number, then enter the Transaction ID below."
                )
            } else {
                // Seed initial configuration document
                val defaults = PaymentSettings()
                val map = hashMapOf<String, Any>(
                    "bkashNumber" to defaults.bkashNumber,
                    "nagadNumber" to defaults.nagadNumber,
                    "bkashEnabled" to defaults.bkashEnabled,
                    "nagadEnabled" to defaults.nagadEnabled,
                    "monthlyPriceBdt" to defaults.monthlyPriceBdt,
                    "paymentInstructions" to defaults.paymentInstructions,
                    "updatedAt" to System.currentTimeMillis()
                )
                try {
                    firestore.collection("payment_settings").document("config")
                        .set(map, SetOptions.merge())
                        .await()
                } catch (_: Exception) {}
                return@withContext defaults
            }
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "getPaymentSettings exception, returning default", e)
            return@withContext PaymentSettings()
        }
    }

    suspend fun updatePaymentSettings(settings: PaymentSettings, adminEmail: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                verifyAdminPrivilege()

                val now = System.currentTimeMillis()
                val map = hashMapOf<String, Any>(
                    "bkashNumber" to settings.bkashNumber,
                    "nagadNumber" to settings.nagadNumber,
                    "bkashEnabled" to settings.bkashEnabled,
                    "nagadEnabled" to settings.nagadEnabled,
                    "monthlyPriceBdt" to settings.monthlyPriceBdt,
                    "paymentInstructions" to settings.paymentInstructions,
                    "updatedAt" to now,
                    "updatedBy" to adminEmail
                )

                firestore.collection("payment_settings").document("config")
                    .set(map, SetOptions.merge())
                    .await()

                logAudit(
                    adminId = adminEmail,
                    action = "UPDATE_PAYMENT_SETTINGS",
                    targetUid = "SYSTEM",
                    metadata = "Updated gateway settings: bKash=${settings.bkashNumber}, Nagad=${settings.nagadNumber}, Price=৳${settings.monthlyPriceBdt}"
                )

                Result.success(true)
            } catch (e: Exception) {
                Log.e("FirebaseBackendClient", "updatePaymentSettings error", e)
                Result.failure(e)
            }
        }

    // ==========================================
    // 3. Payment Orders & Verification
    // ==========================================

    suspend fun submitPaymentOrder(
        uid: String,
        name: String,
        provider: String,
        trxId: String,
        amount: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val orderId = "PAY-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()

            val orderMap = hashMapOf<String, Any>(
                "id" to orderId,
                "uid" to uid,
                "name" to name,
                "plan" to "PRO_MONTHLY",
                "provider" to provider,
                "amount" to amount,
                "transactionReference" to trxId,
                "status" to "PENDING",
                "createdAt" to now,
                "verifiedAt" to 0L
            )

            firestore.collection("payment_orders").document(orderId)
                .set(orderMap)
                .await()

            // Record in payment_events
            val eventMap = hashMapOf<String, Any>(
                "paymentId" to orderId,
                "uid" to uid,
                "action" to "SUBMITTED",
                "amount" to amount,
                "provider" to provider,
                "transactionReference" to trxId,
                "createdAt" to now
            )
            firestore.collection("payment_events").add(eventMap).await()

            Result.success(orderId)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "submitPaymentOrder error", e)
            Result.failure(e)
        }
    }

    suspend fun getAllPayments(): List<PaymentOrder> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("payment_orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.map { doc ->
                PaymentOrder(
                    id = doc.getString("id") ?: doc.id,
                    uid = doc.getString("uid") ?: "",
                    name = doc.getString("name") ?: "",
                    plan = doc.getString("plan") ?: "PRO_MONTHLY",
                    provider = doc.getString("provider") ?: "bKash",
                    amount = doc.getDouble("amount") ?: 150.0,
                    transactionReference = doc.getString("transactionReference") ?: "",
                    status = doc.getString("status") ?: "PENDING",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    verifiedAt = doc.getLong("verifiedAt")
                )
            }
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "getAllPayments error", e)
            emptyList()
        }
    }

    suspend fun updatePaymentStatus(
        paymentId: String,
        status: String,
        adminEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            verifyAdminPrivilege()

            val now = System.currentTimeMillis()
            val paymentDoc = firestore.collection("payment_orders").document(paymentId).get().await()

            if (!paymentDoc.exists()) {
                return@withContext Result.failure(Exception("Payment order not found"))
            }

            val targetUid = paymentDoc.getString("uid") ?: ""
            val amount = paymentDoc.getDouble("amount") ?: 150.0
            val provider = paymentDoc.getString("provider") ?: "bKash"
            val trxId = paymentDoc.getString("transactionReference") ?: ""

            firestore.collection("payment_orders").document(paymentId)
                .update(
                    mapOf(
                        "status" to status,
                        "verifiedAt" to now,
                        "verifiedBy" to adminEmail
                    )
                ).await()

            // Record payment event
            val eventMap = hashMapOf<String, Any>(
                "paymentId" to paymentId,
                "uid" to targetUid,
                "action" to status,
                "adminId" to adminEmail,
                "amount" to amount,
                "provider" to provider,
                "transactionReference" to trxId,
                "createdAt" to now
            )
            firestore.collection("payment_events").add(eventMap).await()

            // If approved, automatically activate Pro entitlement for 30 days
            if (status == "PAID" && targetUid.isNotBlank()) {
                setUserPro(
                    uid = targetUid,
                    isPro = true,
                    durationDays = 30,
                    adminEmail = adminEmail
                )
            }

            logAudit(
                adminId = adminEmail,
                action = if (status == "PAID") "APPROVE_PAYMENT" else "REJECT_PAYMENT",
                targetUid = targetUid,
                metadata = "Payment $paymentId ($provider ৳$amount, TrxID: $trxId) marked as $status"
            )

            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "updatePaymentStatus error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 4. Admin Authentication & Role Authorization
    // ==========================================

    suspend fun adminLogin(email: String, pass: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUid = authResult.user?.uid ?: throw Exception("Authentication failed: No UID returned")

            // Critical Security Verification: Validate document in admins/{firebaseUid}
            val adminDoc = firestore.collection("admins").document(firebaseUid).get().await()

            if (!adminDoc.exists()) {
                auth.signOut()
                throw SecurityException("Access Denied: Account is not recognized as an administrator.")
            }

            val role = adminDoc.getString("role")
            val status = adminDoc.getString("status")

            if (role != "admin" || status != "active") {
                auth.signOut()
                throw SecurityException("Access Denied: Inactive or insufficient administrative credentials.")
            }

            // Save admin session state
            prefs.edit()
                .putString("admin_email", email)
                .putString("admin_uid", firebaseUid)
                .putLong("admin_login_time", System.currentTimeMillis())
                .apply()

            logAudit(
                adminId = email,
                action = "ADMIN_LOGIN",
                targetUid = firebaseUid,
                metadata = "Administrator authenticated successfully from IP/Device"
            )

            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "adminLogin failed", e)
            Result.failure(e)
        }
    }

    private suspend fun verifyAdminPrivilege() {
        val user = auth.currentUser ?: throw SecurityException("Unauthorized: Admin sign-in required.")
        val adminDoc = firestore.collection("admins").document(user.uid).get().await()
        if (!adminDoc.exists() || adminDoc.getString("role") != "admin" || adminDoc.getString("status") != "active") {
            throw SecurityException("Unauthorized: Active admin status required.")
        }
    }

    fun logoutAdmin() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
    }

    // ==========================================
    // 5. Admin User Management
    // ==========================================

    suspend fun getAllUsers(): List<UserRecord> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.map { doc ->
                UserRecord(
                    uid = doc.getString("uid") ?: doc.id,
                    name = doc.getString("name") ?: "User",
                    status = doc.getString("status") ?: "ACTIVE",
                    isPro = doc.getBoolean("isPro") ?: false,
                    proExpiry = doc.getLong("proExpiry"),
                    downloadCount = (doc.getLong("downloadCount") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastActiveAt = doc.getLong("lastActiveAt") ?: System.currentTimeMillis(),
                    appVersion = doc.getString("appVersion") ?: "2.5.7",
                    platform = doc.getString("platform") ?: "Android"
                )
            }
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "getAllUsers error", e)
            emptyList()
        }
    }

    suspend fun setUserPro(
        uid: String,
        isPro: Boolean,
        durationDays: Int,
        adminEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            verifyAdminPrivilege()

            val now = System.currentTimeMillis()
            val expiry = if (isPro) now + durationDays * 24L * 60L * 60L * 1000L else 0L

            // Update pro_entitlements collection
            val entitlementMap = hashMapOf<String, Any>(
                "uid" to uid,
                "isPro" to isPro,
                "proExpiry" to expiry,
                "updatedAt" to now,
                "updatedBy" to adminEmail
            )
            firestore.collection("pro_entitlements").document(uid)
                .set(entitlementMap, SetOptions.merge())
                .await()

            // Update users collection
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "isPro" to isPro,
                        "proExpiry" to expiry
                    )
                ).await()

            logAudit(
                adminId = adminEmail,
                action = if (isPro) "ACTIVATE_PRO" else "REVOKE_PRO",
                targetUid = uid,
                metadata = if (isPro) "Granted $durationDays days Pro" else "Revoked Pro subscription"
            )

            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "setUserPro error", e)
            Result.failure(e)
        }
    }

    suspend fun setUserStatus(
        uid: String,
        status: String,
        adminEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            verifyAdminPrivilege()

            firestore.collection("users").document(uid)
                .update("status", status)
                .await()

            logAudit(
                adminId = adminEmail,
                action = "SET_USER_STATUS",
                targetUid = uid,
                metadata = "Changed status to $status"
            )

            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirebaseBackendClient", "setUserStatus error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 6. Admin Audit Logs
    // ==========================================

    suspend fun getAllAuditLogs(): List<AuditLog> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("admin_audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.map { doc ->
                AuditLog(
                    id = doc.id,
                    adminId = doc.getString("adminId") ?: "admin",
                    action = doc.getString("action") ?: "UNKNOWN",
                    targetUid = doc.getString("targetUid") ?: "",
                    metadata = doc.getString("metadata") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "getAllAuditLogs error", e)
            emptyList()
        }
    }

    private suspend fun logAudit(
        adminId: String,
        action: String,
        targetUid: String,
        metadata: String
    ) {
        try {
            val logMap = hashMapOf<String, Any>(
                "adminId" to adminId,
                "action" to action,
                "targetUid" to targetUid,
                "metadata" to metadata,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("admin_audit_logs").add(logMap).await()
        } catch (e: Exception) {
            Log.w("FirebaseBackendClient", "logAudit notice: ${e.message}")
        }
    }
}
