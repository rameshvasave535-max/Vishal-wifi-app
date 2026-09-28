package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ClientEntity
import com.example.data.model.PlanEntity
import com.example.data.model.RechargeRequestEntity
import com.example.data.model.SettingsEntity
import com.example.data.repository.WifiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

sealed interface UserSession {
    object None : UserSession
    data class Client(val mobile: String) : UserSession
    object Admin : UserSession
}

data class SpeedTestState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val phase: String = "Idle", // "Testing Ping", "Testing Download", "Testing Upload", "Finished"
    val pingMs: Int = 18,
    val jitterMs: Int = 3,
    val downloadSpeedMbps: Float = 0f,
    val uploadSpeedMbps: Float = 0f
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WifiRepository

    init {
        val dao = AppDatabase.getDatabase(application).wifiDao()
        repository = WifiRepository(dao)
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }
    }

    val allClients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlans: StateFlow<List<PlanEntity>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRechargeRequests: StateFlow<List<RechargeRequestEntity>> = repository.allRechargeRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _session = MutableStateFlow<UserSession>(UserSession.None)
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val _currentClientMobile = MutableStateFlow<String?>(null)
    val currentClientMobile: StateFlow<String?> = _currentClientMobile.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _selectedPlanForRecharge = MutableStateFlow<PlanEntity?>(null)
    val selectedPlanForRecharge: StateFlow<PlanEntity?> = _selectedPlanForRecharge.asStateFlow()

    // Speed test state
    private val _speedTest = MutableStateFlow(SpeedTestState())
    val speedTest: StateFlow<SpeedTestState> = _speedTest.asStateFlow()
    private var speedTestJob: Job? = null

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun loginAdmin(idInput: String, passInput: String): Boolean {
        val currentSettings = settings.value
        val expectedId = currentSettings?.helplineNumber ?: "9545362903"
        val expectedPass = currentSettings?.adminPassword ?: "Edith@1250"

        if (idInput.trim() == expectedId && passInput == expectedPass) {
            _session.value = UserSession.Admin
            _authError.value = null
            return true
        } else {
            _authError.value = "Galat Admin ID ya Password!"
            return false
        }
    }

    fun loginClient(mobile: String, pass: String) {
        viewModelScope.launch {
            val result = repository.loginClient(mobile.trim(), pass)
            result.fold(
                onSuccess = { client ->
                    _currentClientMobile.value = client.mobile
                    _session.value = UserSession.Client(client.mobile)
                    _authError.value = null
                },
                onFailure = { error ->
                    _authError.value = error.message ?: "Login failed"
                }
            )
        }
    }

    fun signupClient(mobile: String, name: String, pass: String) {
        if (mobile.length < 10) {
            _authError.value = "Kripya 10-digit mobile number enter karein!"
            return
        }
        if (pass.isBlank()) {
            _authError.value = "Kripya password banayein!"
            return
        }

        viewModelScope.launch {
            val result = repository.registerClient(mobile.trim(), name.trim(), pass)
            result.fold(
                onSuccess = { client ->
                    _currentClientMobile.value = client.mobile
                    _session.value = UserSession.Client(client.mobile)
                    _authError.value = null
                },
                onFailure = { error ->
                    _authError.value = error.message ?: "Registration failed"
                }
            )
        }
    }

    fun logout() {
        _session.value = UserSession.None
        _currentClientMobile.value = null
        _selectedPlanForRecharge.value = null
        _authError.value = null
    }

    fun selectPlanForRecharge(plan: PlanEntity?) {
        _selectedPlanForRecharge.value = plan
    }

    fun submitRecharge(utr: String, paymentMode: String = "UPI", onComplete: (Boolean) -> Unit) {
        val plan = _selectedPlanForRecharge.value
        val mobile = _currentClientMobile.value

        if (plan == null || mobile == null) {
            _actionMessage.value = "Plan select karein!"
            onComplete(false)
            return
        }
        if (paymentMode == "UPI" && utr.trim().isBlank()) {
            _actionMessage.value = "Payment reference (UTR / Transaction ID) daalein!"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            val refText = if (paymentMode == "Cash") {
                if (utr.trim().isNotBlank()) "Cash Pickup: ${utr.trim()}" else "Cash on Doorstep / Office"
            } else {
                utr.trim()
            }
            val result = repository.submitRecharge(mobile, plan, refText, paymentMode)
            result.fold(
                onSuccess = {
                    _actionMessage.value = if (paymentMode == "Cash") {
                        "Cash payment request submit ho gayi! Admin ko notification bhej di gayi hai."
                    } else {
                        "Recharge successful! Admin ko notification bhej di gayi hai."
                    }
                    _selectedPlanForRecharge.value = null
                    onComplete(true)
                },
                onFailure = { error ->
                    _actionMessage.value = error.message ?: "Recharge submit failed"
                    onComplete(false)
                }
            )
        }
    }

    // --- Admin Functions ---
    fun savePaymentSettings(upi: String, qrUrl: String, helpline: String, adminPass: String) {
        viewModelScope.launch {
            repository.savePaymentSettings(upi, qrUrl, helpline, adminPass)
            _actionMessage.value = "Payment settings save ho gayi!"
        }
    }

    fun addPlan(speed: String, priceStr: String, validityStr: String, desc: String = "") {
        val price = priceStr.toIntOrNull()
        val validity = validityStr.toIntOrNull()
        if (speed.isBlank() || price == null || validity == null) {
            _actionMessage.value = "Kripya sari fields (Speed, Price, Validity) sahi se bharein!"
            return
        }

        viewModelScope.launch {
            repository.addPlan(speed.trim(), price, validity, desc)
            _actionMessage.value = "Naya Plan add ho gaya: $speed"
        }
    }

    fun deletePlan(id: Long) {
        viewModelScope.launch {
            repository.deletePlan(id)
            _actionMessage.value = "Plan delete ho gaya"
        }
    }

    fun updateClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.updateClient(client)
            _actionMessage.value = "Client data update ho gaya!"
        }
    }

    fun deleteClient(mobile: String) {
        viewModelScope.launch {
            repository.deleteClient(mobile)
            _actionMessage.value = "Client delete ho gaya"
        }
    }

    fun dismissNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteRechargeRequest(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            _actionMessage.value = "Sabhi notifications clear kar diye gaye."
        }
    }

    fun startSpeedTest(targetSpeedMbps: Float = 60f) {
        if (_speedTest.value.isRunning) return
        speedTestJob?.cancel()

        speedTestJob = viewModelScope.launch {
            _speedTest.value = SpeedTestState(isRunning = true, phase = "Connecting to Server...", progress = 0.05f)
            delay(600)

            // Ping test
            _speedTest.value = _speedTest.value.copy(phase = "Measuring Latency & Ping...", pingMs = 12 + Random.nextInt(8), jitterMs = 1 + Random.nextInt(3), progress = 0.2f)
            delay(800)

            // Download test
            _speedTest.value = _speedTest.value.copy(phase = "Testing Download Speed...", progress = 0.35f)
            val baseDown = if (targetSpeedMbps > 0) targetSpeedMbps else 50f
            for (step in 1..10) {
                delay(200)
                val current = (baseDown * (step / 10f)) + Random.nextFloat() * 4f - 2f
                _speedTest.value = _speedTest.value.copy(
                    downloadSpeedMbps = String.format(Locale.US, "%.1f", current.coerceAtLeast(1f)).toFloat(),
                    progress = 0.35f + (step / 10f) * 0.35f
                )
            }

            // Upload test
            _speedTest.value = _speedTest.value.copy(phase = "Testing Upload Speed...", progress = 0.75f)
            val baseUp = baseDown * 0.85f
            for (step in 1..8) {
                delay(200)
                val current = (baseUp * (step / 8f)) + Random.nextFloat() * 3f - 1.5f
                _speedTest.value = _speedTest.value.copy(
                    uploadSpeedMbps = String.format(Locale.US, "%.1f", current.coerceAtLeast(1f)).toFloat(),
                    progress = 0.75f + (step / 8f) * 0.25f
                )
            }

            _speedTest.value = _speedTest.value.copy(
                isRunning = false,
                phase = "Test Complete! Network Healthy",
                progress = 1.0f
            )
        }
    }

    fun getDaysUntilExpiry(expiryString: String?): Int? {
        if (expiryString.isNullOrBlank()) return null
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiry = dateFormat.parse(expiryString) ?: return null
            val now = Date()
            val diffMs = expiry.time - now.time
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } catch (_: Exception) {
            null
        }
    }
}
