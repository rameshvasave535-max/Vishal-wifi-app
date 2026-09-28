package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClientEntity
import com.example.data.model.PlanEntity
import com.example.data.model.RechargeRequestEntity
import com.example.data.model.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {

    // --- Clients ---
    @Query("SELECT * FROM clients ORDER BY createdAt DESC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE mobile = :mobile LIMIT 1")
    fun getClient(mobile: String): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE mobile = :mobile LIMIT 1")
    suspend fun getClientDirect(mobile: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE mobile = :mobile")
    suspend fun deleteClient(mobile: String)

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getClientsCount(): Int

    // --- Plans ---
    @Query("SELECT * FROM plans ORDER BY price ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun getPlansCount(): Int

    // --- Recharge Requests / Admin Notifications ---
    @Query("SELECT * FROM recharge_requests ORDER BY timestamp DESC")
    fun getAllRechargeRequests(): Flow<List<RechargeRequestEntity>>

    @Query("SELECT * FROM recharge_requests WHERE clientMobile = :mobile ORDER BY timestamp DESC")
    fun getRechargesForClient(mobile: String): Flow<List<RechargeRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRechargeRequest(request: RechargeRequestEntity): Long

    @Update
    suspend fun updateRechargeRequest(request: RechargeRequestEntity)

    @Query("DELETE FROM recharge_requests WHERE id = :id")
    suspend fun deleteRechargeRequest(id: Long)

    @Query("DELETE FROM recharge_requests")
    suspend fun clearAllNotifications()

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)
}
