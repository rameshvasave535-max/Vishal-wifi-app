package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val mobile: String,
    val name: String,
    val password: String,
    val ip: String = "Pending Setup",
    val plan: String? = null,
    val planPrice: Int? = null,
    val expiry: String? = null, // Format: YYYY-MM-DD
    val status: String = "Active", // "Active", "Expiring", "Expired", "Suspended"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val speed: String,
    val price: Int,
    val validityDays: Int,
    val description: String = "Unlimited High Speed Data, No FUP Limit",
    val isPopular: Boolean = false
)

@Entity(tableName = "recharge_requests")
data class RechargeRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientMobile: String,
    val clientName: String,
    val planSpeed: String,
    val amount: Int,
    val validityDays: Int,
    val utr: String,
    val paymentMode: String = "UPI", // "UPI" or "Cash"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending" // "Pending", "Verified", "Rejected", "Cash Received"
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val upiId: String = "9545362903@upi",
    val qrImageUrl: String = "",
    val helplineNumber: String = "9545362903",
    val adminPassword: String = "Edith@1250"
)
