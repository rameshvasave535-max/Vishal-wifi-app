package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientEntity
import com.example.data.model.PlanEntity
import com.example.data.model.RechargeRequestEntity
import com.example.data.model.SettingsEntity
import com.example.ui.components.PlanCard
import com.example.ui.components.QrCodeView
import com.example.ui.components.SpeedTestView
import com.example.ui.theme.WifiAmber
import com.example.ui.theme.WifiBlueDark
import com.example.ui.theme.WifiBluePrimary
import com.example.ui.theme.WifiCyanGlow
import com.example.ui.theme.WifiEmerald
import com.example.ui.theme.WifiEmeraldLight
import com.example.ui.theme.WifiRed
import com.example.ui.viewmodel.SpeedTestState

@Composable
fun ClientDashboardScreen(
    client: ClientEntity,
    plans: List<PlanEntity>,
    settings: SettingsEntity?,
    speedTestState: SpeedTestState,
    selectedPlan: PlanEntity?,
    actionMessage: String?,
    onClearActionMessage: () -> Unit,
    onSelectPlan: (PlanEntity?) -> Unit,
    onSubmitRecharge: (utr: String, paymentMode: String, onDone: (Boolean) -> Unit) -> Unit,
    onRunSpeedTest: (targetSpeed: Float) -> Unit,
    daysUntilExpiry: Int?
) {
    val context = LocalContext.current
    val helpline = settings?.helplineNumber ?: "9545362903"
    val upiId = settings?.upiId ?: "9545362903@upi"
    val qrUrl = settings?.qrImageUrl

    var selectedPaymentMethod by remember { mutableStateOf("UPI") } // "UPI" or "Cash"
    var utrInput by remember { mutableStateOf("") }
    var cashNotesInput by remember { mutableStateOf("") }
    var showPaymentSuccessDialog by remember { mutableStateOf(false) }
    var lastSubmittedMode by remember { mutableStateOf("UPI") }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Expiry Banner Alert (as featured in the HTML prototype)
        if (daysUntilExpiry != null) {
            when {
                daysUntilExpiry < 0 -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = WifiRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alert: Aapka Internet plan expire ho chuka hai!",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Niche diye gaye plan me se choose karke turant recharge karein.",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                daysUntilExpiry in 0..3 -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Expiry Alert",
                                tint = WifiAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alert: Aapka plan agle $daysUntilExpiry din me expire ho raha hai!",
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Nirantar high speed internet ke liye kripya recharge karein.",
                                    color = Color(0xFFB45309),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Healthy connection banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = WifiEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High-Speed Internet Active • $daysUntilExpiry days remaining",
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Client Details Card
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name & Mobile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WELCOME BACK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = client.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WifiBlueDark
                        )
                        Text(
                            text = "📱 ${client.mobile}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = WifiBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Grid: Allocated Static IP & Active Plan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Allocated Static IP Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { copyToClipboard(client.ip, "Static IP") },
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ALLOCATED STATIC IP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = client.ip,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WifiEmerald,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy IP",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Active Plan & Expiry
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "CURRENT PLAN & EXPIRY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = client.plan ?: "None Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WifiBluePrimary
                            )
                            Text(
                                text = if (!client.expiry.isNullOrBlank()) "Valid: ${client.expiry}" else "Recharge required",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // 3. Interactive Speed & Latency Test
        val targetSpeedVal = remember(client.plan) {
            when {
                client.plan?.contains("100") == true -> 98f
                client.plan?.contains("Unlimited") == true -> 150f
                client.plan?.contains("50") == true -> 52f
                client.plan?.contains("30") == true -> 31f
                else -> 45f
            }
        }
        SpeedTestView(
            state = speedTestState,
            targetSpeed = targetSpeedVal,
            onRunTest = { onRunSpeedTest(targetSpeedVal) }
        )

        // 4. Recharge Plans Section
        Text(
            text = "Choose a Plan to Recharge",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = WifiBlueDark
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isAdmin = false,
                    isSelected = selectedPlan?.id == plan.id,
                    onSelectPlan = {
                        onSelectPlan(it)
                    }
                )
            }
        }

        // 5. Payment Box (When a plan is selected)
        if (selectedPlan != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_box_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp, WifiBluePrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Complete Payment",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WifiBlueDark
                            )
                            Text(
                                text = "Selected: ${selectedPlan.speed} • ₹${selectedPlan.price}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WifiEmerald
                            )
                        }

                        TextButton(onClick = { onSelectPlan(null) }) {
                            Text("Cancel", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selector: UPI vs Cash
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPaymentMethod = "UPI" },
                            color = if (selectedPaymentMethod == "UPI") Color.White else Color.Transparent,
                            shadowElevation = if (selectedPaymentMethod == "UPI") 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (selectedPaymentMethod == "UPI") WifiBluePrimary else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Online UPI & QR",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPaymentMethod == "UPI") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedPaymentMethod == "UPI") WifiBluePrimary else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPaymentMethod = "Cash" },
                            color = if (selectedPaymentMethod == "Cash") Color.White else Color.Transparent,
                            shadowElevation = if (selectedPaymentMethod == "Cash") 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (selectedPaymentMethod == "Cash") WifiEmerald else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cash Payment (नकद)",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPaymentMethod == "Cash") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedPaymentMethod == "Cash") WifiEmerald else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedPaymentMethod == "UPI") {
                        // --- UPI / ONLINE PAYMENT FLOW ---
                        Text(
                            text = "Pay using UPI or Scan QR code below:",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR Code & Payment Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            QrCodeView(
                                qrImageUrl = qrUrl,
                                seedString = "$upiId-${selectedPlan.price}",
                                modifier = Modifier.size(150.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // UPI ID Box with Copy & Open UPI Buttons
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("UPI ID:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = upiId,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WifiBluePrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row {
                                    IconButton(onClick = { copyToClipboard(upiId, "UPI ID") }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy UPI ID",
                                            tint = WifiBluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // "Pay via UPI App" launcher button
                        OutlinedButton(
                            onClick = {
                                val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=Vishal%20Wifi%20Internet&am=${selectedPlan.price}&cu=INR&tn=Recharge%20${selectedPlan.speed}")
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                try {
                                    context.startActivity(Intent.createChooser(upiIntent, "Pay with UPI"))
                                } catch (_: Exception) {
                                    copyToClipboard(upiId, "UPI ID")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WifiBluePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open GPay / PhonePe / Paytm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // UTR / Transaction ID input
                        OutlinedTextField(
                            value = utrInput,
                            onValueChange = { utrInput = it },
                            label = { Text("Transaction ID / UTR Number") },
                            placeholder = { Text("e.g. 425619842103") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("utr_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WifiEmerald,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit UPI Recharge Button
                        Button(
                            onClick = {
                                lastSubmittedMode = "UPI"
                                onSubmitRecharge(utrInput, "UPI") { success ->
                                    if (success) {
                                        showPaymentSuccessDialog = true
                                        utrInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_recharge_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WifiEmerald)
                        ) {
                            Text(
                                text = "Confirm & Notify Admin (₹${selectedPlan.price})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // --- CASH PAYMENT FLOW ---
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = WifiEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Doorstep Cash Collection / Pay at Office",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Hamara technician aapke address par aakar ₹${selectedPlan.price} cash collect karega.\n• Ya aap Vishal Wi-Fi local office me jakar cash de sakte hain.\n• Request submit karne par Admin ko turant alert mil jayega.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount to pay in Cash badge
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Cash Amount to Pay:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text("₹${selectedPlan.price} (No Extra Charges)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = WifiBlueDark)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "Pay on Visit",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Address / Notes field
                        OutlinedTextField(
                            value = cashNotesInput,
                            onValueChange = { cashNotesInput = it },
                            label = { Text("Pickup Address / Landmark / Timing") },
                            placeholder = { Text("e.g. Near Shiv Mandir, Room No. 4, Evening 6 PM") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B))
                            },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_address_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WifiEmerald,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit Cash Request Button
                        Button(
                            onClick = {
                                lastSubmittedMode = "Cash"
                                onSubmitRecharge(cashNotesInput, "Cash") { success ->
                                    if (success) {
                                        showPaymentSuccessDialog = true
                                        cashNotesInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_cash_recharge_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WifiEmerald)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Request Cash Pickup (₹${selectedPlan.price})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // WhatsApp cash booking option
                        OutlinedButton(
                            onClick = {
                                val msg = "Hello Vishal Wi-Fi, I want to recharge '${selectedPlan.speed}' (₹${selectedPlan.price}) with CASH PAYMENT. My registered number is ${client.mobile}.${if (cashNotesInput.isNotBlank()) " Pickup Address: $cashNotesInput" else ""}"
                                val waUri = Uri.parse("https://wa.me/91$helpline?text=${Uri.encode(msg)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF047857))
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Cash Pickup on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Quick WhatsApp & Direct Call Support (as prominent in prototype)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFD1FAE5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Need Immediate Help or Wi-Fi Fix?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Directly contact Vishal Wi-Fi Internet Helpline",
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Button
                    Button(
                        onClick = {
                            val msg = "Hello Vishal Wi-Fi, my registered number is ${client.mobile}. I need help."
                            val waUri = Uri.parse("https://wa.me/91$helpline?text=${Uri.encode(msg)}")
                            val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                            context.startActivity(waIntent)
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("WhatsApp Support", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Call Button
                    OutlinedButton(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$helpline"))
                            context.startActivity(callIntent)
                        },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF065F46))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }

    // Success Dialog on Recharge
    if (showPaymentSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = WifiEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lastSubmittedMode == "Cash") "Cash Pickup Request Submitted!" else "Recharge Successful!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (lastSubmittedMode == "Cash") {
                        "Aapki Cash payment request Vishal Wi-Fi Admin ko bhej di gayi hai. Hamara technician aapse sampark karke cash collect karega aur receipt pradan karega!"
                    } else {
                        "Aapka recharge submit ho gaya hai aur broadband validity extend kar di gayi hai! Admin ko notification bhej di gayi hai."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPaymentSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WifiEmerald)
                ) {
                    Text("Theek Hai / OK")
                }
            }
        )
    }
}
