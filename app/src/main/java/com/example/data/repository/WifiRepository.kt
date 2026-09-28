package com.example.data.repository

import com.example.data.db.WifiDao
import com.example.data.model.ClientEntity
import com.example.data.model.PlanEntity
import com.example.data.model.RechargeRequestEntity
import com.example.data.model.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class WifiRepository(private val dao: WifiDao) {

    val allClients: Flow<List<ClientEntity>> = dao.getAllClients()
    val allPlans: Flow<List<PlanEntity>> = dao.getAllPlans()
    val allRechargeRequests: Flow<List<RechargeRequestEntity>> = dao.getAllRechargeRequests()
    val settings: Flow<SettingsEntity?> = dao.getSettings()

    fun getClient(mobile: String): Flow<ClientEntity?> = dao.getClient(mobile)
    fun getRechargesForClient(mobile: String): Flow<List<RechargeRequestEntity>> = dao.getRechargesForClient(mobile)

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // Seed settings if not present
        val currentSettings = dao.getSettingsDirect()
        if (currentSettings == null) {
            dao.insertOrUpdateSettings(
                SettingsEntity(
                    id = 1,
                    upiId = "9545362903@upi",
                    qrImageUrl = "",
                    helplineNumber = "9545362903",
                    adminPassword = "Edith@1250"
                )
            )
        }

        // Seed plans if empty
        if (dao.getPlansCount() == 0) {
            val defaultPlans = listOf(
                PlanEntity(
                    speed = "30 Mbps",
                    price = 399,
                    validityDays = 30,
                    description = "Unlimited High Speed Data • Perfect for WFH & SD streaming",
                    isPopular = false
                ),
                PlanEntity(
                    speed = "50 Mbps",
                    price = 499,
                    validityDays = 30,
                    description = "Most Popular! Smooth 4K streaming & gaming across 5+ devices",
                    isPopular = true
                ),
                PlanEntity(
                    speed = "100 Mbps",
                    price = 699,
                    validityDays = 30,
                    description = "Lightning Fast Fiber • Heavy downloads & uninterrupted video calls",
                    isPopular = false
                ),
                PlanEntity(
                    speed = "Unlimited Ultra",
                    price = 999,
                    validityDays = 30,
                    description = "Gigabit-Ready Ultra Tier • Dedicated priority bandwidth",
                    isPopular = false
                )
            )
            dao.insertPlans(defaultPlans)
        }

        // Seed sample clients if empty so user can experience immediately
        if (dao.getClientsCount() == 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            
            // Client 1: Expiring in 2 days (demonstrates the urgency banner from HTML)
            cal.add(Calendar.DAY_OF_YEAR, 2)
            val expirySoon = dateFormat.format(cal.time)

            // Client 2: Expiring in 28 days
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, 28)
            val expiryHealthy = dateFormat.format(cal.time)

            val demoClients = listOf(
                ClientEntity(
                    mobile = "9876543210",
                    name = "Rahul Sharma",
                    password = "pass",
                    ip = "192.168.1.102",
                    plan = "50 Mbps",
                    planPrice = 499,
                    expiry = expirySoon,
                    status = "Active"
                ),
                ClientEntity(
                    mobile = "9123456780",
                    name = "Pooja Patil",
                    password = "pass",
                    ip = "192.168.1.105",
                    plan = "100 Mbps",
                    planPrice = 699,
                    expiry = expiryHealthy,
                    status = "Active"
                )
            )
            demoClients.forEach { dao.insertClient(it) }
        }
    }

    // --- Authentication ---
    suspend fun registerClient(mobile: String, name: String, password: String):Result<ClientEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getClientDirect(mobile)
        if (existing != null) {
            return@withContext Result.failure(Exception("Ye number pehle se registered hai! Please login karein."))
        }
        val randomHost = Random.nextInt(15, 240)
        val newClient = ClientEntity(
            mobile = mobile,
            name = if (name.isNotBlank()) name else "Client ${mobile.takeLast(4)}",
            password = password,
            ip = "192.168.1.$randomHost",
            plan = null,
            planPrice = null,
            expiry = null,
            status = "Pending Plan"
        )
        dao.insertClient(newClient)
        Result.success(newClient)
    }

    suspend fun loginClient(mobile: String, password: String): Result<ClientEntity> = withContext(Dispatchers.IO) {
        val client = dao.getClientDirect(mobile)
        if (client == null || client.password != password) {
            return@withContext Result.failure(Exception("Invalid mobile number ya password!"))
        }
        Result.success(client)
    }

    // --- Recharge Flow ---
    suspend fun submitRecharge(
        clientMobile: String,
        plan: PlanEntity,
        utr: String,
        paymentMode: String = "UPI"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = dao.getClientDirect(clientMobile)
            ?: return@withContext Result.failure(Exception("Client not found"))

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // If client already has an active future expiry, add to that date; otherwise add from today
        try {
            if (!client.expiry.isNullOrBlank()) {
                val currentExpiry = dateFormat.parse(client.expiry)
                if (currentExpiry != null && currentExpiry.after(Date())) {
                    cal.time = currentExpiry
                }
            }
        } catch (_: Exception) {
            cal.time = Date()
        }

        cal.add(Calendar.DAY_OF_YEAR, plan.validityDays)
        val newExpiry = dateFormat.format(cal.time)

        val updatedClient = client.copy(
            plan = plan.speed,
            planPrice = plan.price,
            expiry = newExpiry,
            status = "Active"
        )
        dao.updateClient(updatedClient)

        // Record recharge notification
        val request = RechargeRequestEntity(
            clientMobile = client.mobile,
            clientName = client.name,
            planSpeed = plan.speed,
            amount = plan.price,
            validityDays = plan.validityDays,
            utr = utr,
            paymentMode = paymentMode,
            timestamp = System.currentTimeMillis(),
            status = if (paymentMode == "Cash") "Cash Pending" else "Pending"
        )
        dao.insertRechargeRequest(request)

        Result.success(Unit)
    }

    // --- Admin Operations ---
    suspend fun addPlan(speed: String, price: Int, validityDays: Int, description: String = "") = withContext(Dispatchers.IO) {
        val desc = if (description.isNotBlank()) description else "High speed fiber broadband unlimited"
        dao.insertPlan(
            PlanEntity(
                speed = speed,
                price = price,
                validityDays = validityDays,
                description = desc
            )
        )
    }

    suspend fun updatePlan(plan: PlanEntity) = withContext(Dispatchers.IO) {
        dao.updatePlan(plan)
    }

    suspend fun deletePlan(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePlan(id)
    }

    suspend fun updateClient(client: ClientEntity) = withContext(Dispatchers.IO) {
        dao.updateClient(client)
    }

    suspend fun addClient(client: ClientEntity) = withContext(Dispatchers.IO) {
        dao.insertClient(client)
    }

    suspend fun deleteClient(mobile: String) = withContext(Dispatchers.IO) {
        dao.deleteClient(mobile)
    }

    suspend fun savePaymentSettings(upi: String, qrUrl: String, helpline: String, adminPass: String) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateSettings(
            SettingsEntity(
                id = 1,
                upiId = upi.trim(),
                qrImageUrl = qrUrl.trim(),
                helplineNumber = helpline.trim(),
                adminPassword = adminPass.trim()
            )
        )
    }

    suspend fun updateRechargeStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        // Can be "Verified" or "Rejected"
    }

    suspend fun deleteRechargeRequest(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteRechargeRequest(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        dao.clearAllNotifications()
    }
}
