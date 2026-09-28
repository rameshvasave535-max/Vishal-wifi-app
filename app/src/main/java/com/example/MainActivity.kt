package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TopNavBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClientDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UserSession
import com.example.ui.viewmodel.WifiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VishalWifiApp()
            }
        }
    }
}

@Composable
fun VishalWifiApp(viewModel: WifiViewModel = viewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val notifications by viewModel.allRechargeRequests.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val selectedPlan by viewModel.selectedPlanForRecharge.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTest.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    // Handle back button when logged in
    BackHandler(enabled = session !is UserSession.None) {
        viewModel.logout()
    }

    val helpline = settings?.helplineNumber ?: "9545362903"
    val isLoggedIn = session !is UserSession.None
    val userRole = when (session) {
        is UserSession.Client -> "Client"
        is UserSession.Admin -> "Admin"
        else -> null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopNavBar(
                helpline = helpline,
                isLoggedIn = isLoggedIn,
                userRole = userRole,
                onLogout = { viewModel.logout() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val currentSession = session) {
                is UserSession.None -> {
                    AuthScreen(
                        authError = authError,
                        onClearError = { viewModel.clearAuthError() },
                        onClientLogin = { mob, pass -> viewModel.loginClient(mob, pass) },
                        onClientSignup = { mob, name, pass -> viewModel.signupClient(mob, name, pass) },
                        onAdminLogin = { id, pass -> viewModel.loginAdmin(id, pass) }
                    )
                }

                is UserSession.Client -> {
                    val client = clients.find { it.mobile == currentSession.mobile }
                    if (client != null) {
                        val daysRemaining = viewModel.getDaysUntilExpiry(client.expiry)
                        ClientDashboardScreen(
                            client = client,
                            plans = plans,
                            settings = settings,
                            speedTestState = speedTestState,
                            selectedPlan = selectedPlan,
                            actionMessage = actionMessage,
                            onClearActionMessage = { viewModel.clearActionMessage() },
                            onSelectPlan = { viewModel.selectPlanForRecharge(it) },
                            onSubmitRecharge = { utr, mode, onDone ->
                                viewModel.submitRecharge(utr, mode, onDone)
                            },
                            onRunSpeedTest = { target ->
                                viewModel.startSpeedTest(target)
                            },
                            daysUntilExpiry = daysRemaining
                        )
                    }
                }

                is UserSession.Admin -> {
                    AdminDashboardScreen(
                        clients = clients,
                        plans = plans,
                        notifications = notifications,
                        settings = settings,
                        onSavePaymentSettings = { upi, qr, help, pass ->
                            viewModel.savePaymentSettings(upi, qr, help, pass)
                        },
                        onAddPlan = { speed, price, validity, desc ->
                            viewModel.addPlan(speed, price, validity, desc)
                        },
                        onDeletePlan = { id -> viewModel.deletePlan(id) },
                        onUpdateClient = { updated -> viewModel.updateClient(updated) },
                        onAddClient = { newClient ->
                            viewModel.signupClient(newClient.mobile, newClient.name, newClient.password)
                        },
                        onDeleteClient = { mob -> viewModel.deleteClient(mob) },
                        onDismissNotification = { id -> viewModel.dismissNotification(id) },
                        onClearAllNotifications = { viewModel.clearAllNotifications() }
                    )
                }
            }
        }
    }
}
