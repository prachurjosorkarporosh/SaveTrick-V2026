package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.PaymentOrder
import com.example.data.model.PaymentSettings
import com.example.data.model.UserRecord
import com.example.data.repository.SaveTrickRepository
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VividCyan
import com.example.ui.theme.WarningOrange
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: SaveTrickRepository,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Users", "Payments", "Payment Settings", "Audit Logs")

    var users by remember { mutableStateOf<List<UserRecord>>(emptyList()) }
    var payments by remember { mutableStateOf<List<PaymentOrder>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    var paymentSettings by remember { mutableStateOf(PaymentSettings()) }
    val totalDownloads by repository.totalDownloadsCount.collectAsState(initial = 0)

    val adminEmail = repository.supabaseClient.adminEmail ?: "admin"

    fun reloadData() {
        scope.launch {
            users = repository.supabaseClient.getAllUsers()
            payments = repository.supabaseClient.getAllPayments()
            auditLogs = repository.supabaseClient.getAllAuditLogs()
            paymentSettings = repository.supabaseClient.getPaymentSettings()
        }
    }

    LaunchedEffect(Unit) {
        reloadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SaveTrick Admin Portal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = adminEmail,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            repository.supabaseClient.logoutAdmin()
                            onLogout()
                        },
                        modifier = Modifier.testTag("btn_admin_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.admin_logout),
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            reloadData()
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> AdminOverviewTab(
                        users = users,
                        payments = payments,
                        auditLogs = auditLogs,
                        totalDownloads = totalDownloads
                    )
                    1 -> AdminUsersTab(
                        users = users,
                        adminId = adminEmail,
                        onUpdateUser = { uid, isPro, status ->
                            scope.launch {
                                if (isPro != null) {
                                    repository.supabaseClient.setUserPro(uid, isPro, 30, adminEmail)
                                }
                                if (status != null) {
                                    repository.supabaseClient.setUserStatus(uid, status, adminEmail)
                                }
                                reloadData()
                                Toast.makeText(context, "User updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    2 -> AdminPaymentsTab(
                        payments = payments,
                        onApprove = { id ->
                            scope.launch {
                                repository.supabaseClient.updatePaymentStatus(id, "PAID", adminEmail)
                                reloadData()
                                Toast.makeText(context, "Payment approved & Pro granted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReject = { id ->
                            scope.launch {
                                repository.supabaseClient.updatePaymentStatus(id, "REJECTED", adminEmail)
                                reloadData()
                                Toast.makeText(context, "Payment rejected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    3 -> AdminSettingsTab(
                        initialSettings = paymentSettings,
                        onSave = { updated ->
                            scope.launch {
                                repository.supabaseClient.updatePaymentSettings(updated, adminEmail)
                                paymentSettings = updated
                                reloadData()
                                Toast.makeText(context, "Payment settings saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    4 -> AdminAuditLogsTab(auditLogs = auditLogs)
                }
            }
        }
    }
}

// 1. Dashboard Tab (9 metrics + recent activity)
@Composable
private fun AdminOverviewTab(
    users: List<UserRecord>,
    payments: List<PaymentOrder>,
    auditLogs: List<AuditLog>,
    totalDownloads: Int
) {
    val scrollState = rememberScrollState()

    val totalUsers = users.size
    val activeUsers = users.count { it.status == "ACTIVE" }
    val suspendedUsers = users.count { it.status == "SUSPENDED" }
    val proUsers = users.count { it.isPro }
    val freeUsers = totalUsers - proUsers
    val pendingPayments = payments.count { it.status == "PENDING" }
    val paidPayments = payments.count { it.status == "PAID" }
    val rejectedPayments = payments.count { it.status == "REJECTED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "System Metrics Overview",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // 9 Metrics Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Total Users", totalUsers.toString(), ElectricCyan, Modifier.weight(1f))
                MetricCard("Active Users", activeUsers.toString(), SuccessGreen, Modifier.weight(1f))
                MetricCard("Suspended", suspendedUsers.toString(), ErrorRed, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Pro Users", proUsers.toString(), ElectricBlue, Modifier.weight(1f))
                MetricCard("Free Users", freeUsers.toString(), Color.Gray, Modifier.weight(1f))
                MetricCard("Downloads", totalDownloads.toString(), VividCyan, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Pending Pay", pendingPayments.toString(), WarningOrange, Modifier.weight(1f))
                MetricCard("Paid Pay", paidPayments.toString(), SuccessGreen, Modifier.weight(1f))
                MetricCard("Rejected Pay", rejectedPayments.toString(), ErrorRed, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Recent Admin Activity",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        auditLogs.take(5).forEach { log ->
            AuditLogCard(log)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

// 2. Users Tab
@Composable
private fun AdminUsersTab(
    users: List<UserRecord>,
    adminId: String,
    onUpdateUser: (uid: String, isPro: Boolean?, status: String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = users.filter {
        it.uid.contains(searchQuery, ignoreCase = true) ||
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by UID or Name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_user_search")
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.uid }) { user ->
                    UserManagementCard(user = user, onUpdateUser = onUpdateUser)
                }
            }
        }
    }
}

@Composable
private fun UserManagementCard(
    user: UserRecord,
    onUpdateUser: (uid: String, isPro: Boolean?, status: String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "UID: ${user.uid}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = if (user.isPro) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (user.isPro) "PRO" else "FREE",
                            color = if (user.isPro) SuccessGreen else Color.Gray,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = if (user.status == "ACTIVE") SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = user.status,
                            color = if (user.status == "ACTIVE") SuccessGreen else ErrorRed,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "Downloads: ${user.downloadCount} • App: v${user.appVersion} (${user.platform})",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Text(
                text = "Joined: ${FormatUtils.formatDate(user.createdAt)} • Active: ${FormatUtils.formatDate(user.lastActiveAt)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!user.isPro) {
                    Button(
                        onClick = { onUpdateUser(user.uid, true, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grant Pro", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onUpdateUser(user.uid, false, null) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revoke Pro", fontSize = 12.sp, color = ErrorRed)
                    }
                }

                if (user.status == "ACTIVE") {
                    OutlinedButton(
                        onClick = { onUpdateUser(user.uid, null, "SUSPENDED") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Suspend", fontSize = 12.sp, color = ErrorRed)
                    }
                } else {
                    Button(
                        onClick = { onUpdateUser(user.uid, null, "ACTIVE") },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unsuspend", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 3. Payments Tab
@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentOrder>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No payments submitted", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(payments, key = { it.id }) { order ->
                PaymentOrderCard(order = order, onApprove = { onApprove(order.id) }, onReject = { onReject(order.id) })
            }
        }
    }
}

@Composable
private fun PaymentOrderCard(
    order: PaymentOrder,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${order.provider} • ৳${order.amount.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "User: ${order.name} (${order.uid})",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }

                Surface(
                    color = when (order.status) {
                        "PAID" -> SuccessGreen.copy(alpha = 0.15f)
                        "REJECTED" -> ErrorRed.copy(alpha = 0.15f)
                        else -> WarningOrange.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.status,
                        color = when (order.status) {
                            "PAID" -> SuccessGreen
                            "REJECTED" -> ErrorRed
                            else -> WarningOrange
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "TrxID: ${order.transactionReference}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Text(
                text = "Date: ${FormatUtils.formatDate(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (order.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve & Pro", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, color = ErrorRed)
                    }
                }
            }
        }
    }
}

// 4. Payment Settings Tab
@Composable
private fun AdminSettingsTab(
    initialSettings: PaymentSettings,
    onSave: (PaymentSettings) -> Unit
) {
    var bkashNumber by remember { mutableStateOf(initialSettings.bkashNumber) }
    var nagadNumber by remember { mutableStateOf(initialSettings.nagadNumber) }
    var bkashEnabled by remember { mutableStateOf(initialSettings.bkashEnabled) }
    var nagadEnabled by remember { mutableStateOf(initialSettings.nagadEnabled) }
    var monthlyPrice by remember { mutableStateOf(initialSettings.monthlyPriceBdt.toString()) }
    var instructions by remember { mutableStateOf(initialSettings.paymentInstructions) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "bKash & Nagad Payment Configuration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // bKash Number
        OutlinedTextField(
            value = bkashNumber,
            onValueChange = { bkashNumber = it },
            label = { Text("bKash Merchant/Personal Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable bKash Gateway", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = bkashEnabled,
                onCheckedChange = { bkashEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
            )
        }

        // Nagad Number
        OutlinedTextField(
            value = nagadNumber,
            onValueChange = { nagadNumber = it },
            label = { Text("Nagad Merchant/Personal Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Nagad Gateway", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = nagadEnabled,
                onCheckedChange = { nagadEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
            )
        }

        // Monthly Price (BDT)
        OutlinedTextField(
            value = monthlyPrice,
            onValueChange = { monthlyPrice = it },
            label = { Text("Pro Monthly Price (BDT)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Instructions
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("Payment Instructions for Users") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val price = monthlyPrice.toIntOrNull() ?: 150
                val updated = initialSettings.copy(
                    bkashNumber = bkashNumber.trim(),
                    nagadNumber = nagadNumber.trim(),
                    bkashEnabled = bkashEnabled,
                    nagadEnabled = nagadEnabled,
                    monthlyPriceBdt = price,
                    paymentInstructions = instructions.trim()
                )
                onSave(updated)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Payment Settings")
        }
    }
}

// 5. Audit Logs Tab
@Composable
private fun AdminAuditLogsTab(auditLogs: List<AuditLog>) {
    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audit records available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLogs, key = { it.id }) { log ->
                AuditLogCard(log)
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLog) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElectricCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.action,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = FormatUtils.formatDate(log.createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Text(
                    text = log.metadata,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
                Text(
                    text = "Admin: ${log.adminId} • Target: ${log.targetUid}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
