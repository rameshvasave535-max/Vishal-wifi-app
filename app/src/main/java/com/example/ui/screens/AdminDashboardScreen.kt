package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientEntity
import com.example.data.model.PlanEntity
import com.example.data.model.RechargeRequestEntity
import com.example.data.model.SettingsEntity
import com.example.ui.components.PlanCard
import com.example.ui.theme.WifiAmber
import com.example.ui.theme.WifiBlueDark
import com.example.ui.theme.WifiBluePrimary
import com.example.ui.theme.WifiCyanGlow
import com.example.ui.theme.WifiEmerald
import com.example.ui.theme.WifiEmeraldLight
import com.example.ui.theme.WifiRed

@Composable
fun AdminDashboardScreen(
    clients: List<ClientEntity>,
    plans: List<PlanEntity>,
    notifications: List<RechargeRequestEntity>,
    settings: SettingsEntity?,
    onSavePaymentSettings: (upi: String, qrUrl: String, helpline: String, pass: String) -> Unit,
    onAddPlan: (speed: String, price: String, validity: String, desc: String) -> Unit,
    onDeletePlan: (id: Long) -> Unit,
    onUpdateClient: (client: ClientEntity) -> Unit,
    onAddClient: (client: ClientEntity) -> Unit,
    onDeleteClient: (mobile: String) -> Unit,
    onDismissNotification: (id: Long) -> Unit,
    onClearAllNotifications: () -> Unit
) {
    val context = LocalContext.current

    // Payment setup state
    var upiInput by remember(settings) { mutableStateOf(settings?.upiId ?: "9545362903@upi") }
    var qrUrlInput by remember(settings) { mutableStateOf(settings?.qrImageUrl ?: "") }
    var helplineInput by remember(settings) { mutableStateOf(settings?.helplineNumber ?: "9545362903") }
    var adminPassInput by remember(settings) { mutableStateOf(settings?.adminPassword ?: "Edith@1250") }

    // Plan add form state
    var newSpeed by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newValidity by remember { mutableStateOf("30") }
    var newDesc by remember { mutableStateOf("") }

    // Client search
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var showAddClientDialog by remember { mutableStateOf(false) }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery) ||
            it.ip.contains(searchQuery, ignoreCase = true) ||
            (it.plan ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. KPI Overview Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminStatCard(
                title = "Total Clients",
                value = "${clients.size}",
                icon = Icons.Default.Person,
                accentColor = WifiBluePrimary,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Active Plans",
                value = "${clients.count { !it.plan.isNullOrBlank() }}",
                icon = Icons.Default.Wifi,
                accentColor = WifiEmerald,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Recharges",
                value = "${notifications.size}",
                icon = Icons.Default.Notifications,
                accentColor = WifiAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Notifications Alert Panel (#admin-notifications)
        if (notifications.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent Recharge Notifications (${notifications.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                        }

                        TextButton(
                            onClick = onClearAllNotifications,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("Clear All", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    notifications.take(6).forEach { notif ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (notif.paymentMode == "Cash") Color(0xFFDCFCE7) else Color(0xFFDBEAFE)
                                        ) {
                                            Text(
                                                text = if (notif.paymentMode == "Cash") "💵 CASH" else "📱 UPI",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (notif.paymentMode == "Cash") Color(0xFF15803D) else Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${notif.clientName} (${notif.clientMobile})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF78350F)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Plan: ${notif.planSpeed} • ₹${notif.amount} | ${if (notif.paymentMode == "Cash") "Details" else "UTR"}: ${notif.utr}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Call client
                                    IconButton(
                                        onClick = {
                                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${notif.clientMobile}"))
                                            context.startActivity(callIntent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // Dismiss notification
                                    IconButton(
                                        onClick = { onDismissNotification(notif.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF92400E),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Payment Setup (UPI & QR) Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = WifiBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment Setup (UPI & QR)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WifiBlueDark
                    )
                }

                OutlinedTextField(
                    value = upiInput,
                    onValueChange = { upiInput = it },
                    label = { Text("Custom UPI ID") },
                    placeholder = { Text("e.g. 9545362903@upi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_upi_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WifiBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                OutlinedTextField(
                    value = qrUrlInput,
                    onValueChange = { qrUrlInput = it },
                    label = { Text("QR Code Image URL (Optional)") },
                    placeholder = { Text("https://your-qr-image-url.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_qr_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WifiBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = helplineInput,
                        onValueChange = { helplineInput = it },
                        label = { Text("Helpline No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WifiBluePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    OutlinedTextField(
                        value = adminPassInput,
                        onValueChange = { adminPassInput = it },
                        label = { Text("Admin Pass") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WifiBluePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                Button(
                    onClick = {
                        onSavePaymentSettings(upiInput, qrUrlInput, helplineInput, adminPassInput)
                        Toast.makeText(context, "Payment settings save ho gayi!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_payment_settings_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WifiBluePrimary)
                ) {
                    Text("Save Payment Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Add / Edit Plans Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = WifiBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Broadband Plan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WifiBlueDark
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newSpeed,
                        onValueChange = { newSpeed = it },
                        label = { Text("Speed") },
                        placeholder = { Text("e.g. 75 Mbps") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_plan_speed_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = it },
                        label = { Text("Price (₹)") },
                        placeholder = { Text("599") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_plan_price_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newValidity,
                        onValueChange = { newValidity = it },
                        label = { Text("Validity (days)") },
                        placeholder = { Text("30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_plan_validity_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Features / Description") },
                        placeholder = { Text("Unlimited bufferless") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Button(
                    onClick = {
                        onAddPlan(newSpeed, newPrice, newValidity, newDesc)
                        newSpeed = ""
                        newPrice = ""
                        newDesc = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_plan_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WifiEmerald)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Plan", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Active Available Plans (${plans.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEach { p ->
                        PlanCard(
                            plan = p,
                            isAdmin = true,
                            onDeletePlan = onDeletePlan
                        )
                    }
                }
            }
        }

        // 5. Registered Clients (Data & Static IP Editor)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = WifiBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registered Clients (${clients.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WifiBlueDark
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddClientDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Client", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, mobile, or IP...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WifiBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Client List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (filteredClients.isEmpty()) {
                        Text(
                            text = "No clients matching criteria.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        filteredClients.forEach { client ->
                            ClientRowCard(
                                client = client,
                                onEdit = { editingClient = client },
                                onDelete = { onDeleteClient(client.mobile) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Edit Client Data / IP Dialog
    if (editingClient != null) {
        val client = editingClient!!
        var editName by remember(client) { mutableStateOf(client.name) }
        var editIp by remember(client) { mutableStateOf(client.ip) }
        var editPlan by remember(client) { mutableStateOf(client.plan ?: "") }
        var editExpiry by remember(client) { mutableStateOf(client.expiry ?: "") }
        var editStatus by remember(client) { mutableStateOf(client.status) }

        AlertDialog(
            onDismissRequest = { editingClient = null },
            title = {
                Text("Edit Client Data & Static IP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Mobile: ${client.mobile}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Client Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_name")
                    )

                    OutlinedTextField(
                        value = editIp,
                        onValueChange = { editIp = it },
                        label = { Text("Assigned Static IP") },
                        placeholder = { Text("e.g. 192.168.1.105") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_client_ip")
                    )

                    OutlinedTextField(
                        value = editPlan,
                        onValueChange = { editPlan = it },
                        label = { Text("Active Broadband Plan") },
                        placeholder = { Text("e.g. 50 Mbps") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editExpiry,
                        onValueChange = { editExpiry = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        placeholder = { Text("2026-10-27") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editStatus,
                        onValueChange = { editStatus = it },
                        label = { Text("Status (Active / Suspended)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = client.copy(
                            name = editName.trim(),
                            ip = editIp.trim(),
                            plan = if (editPlan.isNotBlank()) editPlan.trim() else null,
                            expiry = if (editExpiry.isNotBlank()) editExpiry.trim() else null,
                            status = editStatus.trim()
                        )
                        onUpdateClient(updated)
                        editingClient = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WifiBluePrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingClient = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Client Dialog
    if (showAddClientDialog) {
        var newClientMobile by remember { mutableStateOf("") }
        var newClientName by remember { mutableStateOf("") }
        var newClientIp by remember { mutableStateOf("192.168.1.") }
        var newClientPass by remember { mutableStateOf("123456") }
        var newClientPlan by remember { mutableStateOf("50 Mbps") }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = {
                Text("Add New Client Provisioning", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newClientMobile,
                        onValueChange = { newClientMobile = it },
                        label = { Text("Mobile Number (10 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = { newClientName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newClientIp,
                        onValueChange = { newClientIp = it },
                        label = { Text("Static IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newClientPlan,
                        onValueChange = { newClientPlan = it },
                        label = { Text("Initial Plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClientMobile.length >= 10 && newClientName.isNotBlank()) {
                            val client = ClientEntity(
                                mobile = newClientMobile.trim(),
                                name = newClientName.trim(),
                                password = newClientPass.trim(),
                                ip = newClientIp.trim(),
                                plan = newClientPlan.trim(),
                                expiry = null,
                                status = "Active"
                            )
                            onAddClient(client)
                            showAddClientDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WifiEmerald)
                ) {
                    Text("Provision Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun ClientRowCard(
    client: ClientEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = client.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "📱 ${client.mobile}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (client.status == "Active") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = client.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (client.status == "Active") Color(0xFF15803D) else Color(0xFFB91C1C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Assigned Static IP:", fontSize = 9.sp, color = Color(0xFF64748B))
                    Text(
                        text = client.ip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WifiEmerald,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text("Plan & Expiry:", fontSize = 9.sp, color = Color(0xFF64748B))
                    Text(
                        text = "${client.plan ?: "No Plan"} (${client.expiry ?: "N/A"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WifiBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: WhatsApp, Call, Edit Data/IP, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Client
                IconButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.mobile}"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call Client",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // WhatsApp Client
                IconButton(
                    onClick = {
                        val msg = "Hello ${client.name}, this is Vishal Wi-Fi Internet management."
                        val waUri = Uri.parse("https://wa.me/91${client.mobile}?text=${Uri.encode(msg)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "WhatsApp Bill / Message",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Edit Data / IP Button
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Data / IP", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Delete Client Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
