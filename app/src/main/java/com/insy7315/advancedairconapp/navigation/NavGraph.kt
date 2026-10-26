// app/src/main/java/com/insy7315/advancedairconapp/navigation/NavGraph.kt
package com.insy7315.advancedairconapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.insy7315.advancedairconapp.auth.AuthState
import com.insy7315.advancedairconapp.auth.AuthViewModel
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.ui.screens.*
import com.insy7315.advancedairconapp.ui.theme.ThemeState
import com.insy7315.advancedairconapp.viewmodels.*

@Composable
fun NavGraph(
    navController: NavHostController,
    navManager: NavManager,
    themeState: ThemeState,
    onThemeChange: (ThemeState) -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val quoteViewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context), context)
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (!hasNavigated) {
                    hasNavigated = true
                    navManager.navigateToMain()
                }
            }
            is AuthState.Unauthenticated -> {
                hasNavigated = false
                navManager.navigateToLogin()
            }
            else -> { }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavManager.Destination.Login.route
    ) {
        composable(NavManager.Destination.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.Fingerprint.route) {
            FingerprintScreen(
                viewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.Main.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            val userRole = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.role.name
                else -> "TECHNICIAN"
            }

            MainScreen(
                userId = userId,
                userRole = userRole,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.Products.route) {
            ProductListScreen(
                viewModel = productViewModel,
                navManager = navManager,
                onBackToDashboard = { navManager.navigateToMain() }
            )
        }

        composable(NavManager.Destination.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                navManager = navManager,
                onThemeChange = onThemeChange,
                currentTheme = themeState
            )
        }

        composable(NavManager.Destination.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.EditProfile.route) {
            EditProfileScreen(
                authViewModel = authViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.Notifications.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            val notificationViewModel: NotificationViewModel = viewModel(
                factory = NotificationViewModel.Factory(context, userId)
            )
            NotificationScreen(
                viewModel = notificationViewModel,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.AddBuilding.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            AddBuildingScreen(
                viewModel = quoteViewModel,
                userId = userId,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.ServiceRequest.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            ServiceRequestScreen(
                viewModel = quoteViewModel,
                userId = userId,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.QuoteHistory.route) {
            QuoteHistoryScreen(
                navManager = navManager
            )
        }

        composable(NavManager.Destination.BTUCalculator.route) {
            BTUCalculatorScreen(navManager = navManager)
        }

        composable(NavManager.Destination.CreateQuote.route) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")?.toIntOrNull() ?: 0
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            CreateQuoteScreen(
                viewModel = quoteViewModel,
                requestId = requestId,
                technicianId = userId,
                navManager = navManager
            )
        }

        // Edit quote pricing screen
        composable(NavManager.Destination.EditQuotePricing.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId")?.toIntOrNull() ?: 0
            EditQuotePricingScreen(
                viewModel = quoteViewModel,
                quoteId = quoteId,
                navManager = navManager
            )
        }

        composable(NavManager.Destination.MyQuotes.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            QuotesListScreen(
                viewModel = quoteViewModel,
                userId = userId,
                isCustomer = true,
                navManager = navManager,
                onBackToDashboard = { navManager.navigateToMain() }
            )
        }

        composable(NavManager.Destination.PendingRequests.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            QuotesListScreen(
                viewModel = quoteViewModel,
                userId = userId,
                isCustomer = false,
                navManager = navManager,
                onBackToDashboard = { navManager.navigateToMain() }
            )
        }

        composable(NavManager.Destination.ServiceBookings.route) {
            val userId = when (authState) {
                is AuthState.Authenticated -> (authState as AuthState.Authenticated).user.uid
                else -> ""
            }
            ServiceBookingsScreen(
                userId = userId,
                navManager = navManager,
                onBackToDashboard = { navManager.navigateToMain() }
            )
        }

        composable(NavManager.Destination.CreateJobCard.route) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0
            CreateJobCardScreen(
                jobId = jobId,
                navManager = navManager
            )
        }

        // ============================================================
        // PAYMENT SCREEN — loads quote + job from Room, then charges
        // ============================================================
        composable(NavManager.Destination.Payment.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId")?.toIntOrNull() ?: 0
            val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0

            val userName = when (authState) {
                is AuthState.Authenticated ->
                    (authState as AuthState.Authenticated).user.displayName
                        ?.substringBefore("|")?.trim()
                        ?.ifBlank { "Customer" } ?: "Customer"
                else -> "Customer"
            }
            val userEmail = when (authState) {
                is AuthState.Authenticated ->
                    (authState as AuthState.Authenticated).user.email
                else -> ""
            }

            // Load the quote + job from Room on the IO dispatcher
            var quote by remember { mutableStateOf<Quote?>(null) }
            var job by remember { mutableStateOf<Job?>(null) }
            var loaded by remember { mutableStateOf(false) }

            LaunchedEffect(quoteId, jobId) {
                quote = quoteViewModel.getQuoteById(quoteId)
                job = quoteViewModel.getJobById(jobId)
                loaded = true
            }

            if (!loaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val q = quote
                val j = job
                if (q == null || j == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Could not load order details",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    PaymentScreen(
                        quote = q,
                        job = j,
                        customerEmail = userEmail,
                        customerName = userName,
                        technicianName = "Technician",
                        navManager = navManager,
                        onPaymentComplete = {
                            // Go back to the dashboard after the customer
                            // dismisses the success screen.
                            navManager.navigateToMain()
                        }
                    )
                }
            }
        }

        // ============================================================
        // RECEIPT VIEWER — technicians re-open old receipts
        // ============================================================
        composable(NavManager.Destination.ReceiptViewer.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId")?.toIntOrNull() ?: 0

            val userName = when (authState) {
                is AuthState.Authenticated ->
                    (authState as AuthState.Authenticated).user.displayName
                        ?.substringBefore("|")?.trim()
                        ?.ifBlank { "User" } ?: "User"
                else -> "User"
            }

            var quote by remember { mutableStateOf<Quote?>(null) }
            var job by remember { mutableStateOf<Job?>(null) }
            var loaded by remember { mutableStateOf(false) }

            LaunchedEffect(quoteId) {
                val q = quoteViewModel.getQuoteById(quoteId)
                quote = q
                if (q != null) {
                    // Find the job that belongs to this quote
                    job = quoteViewModel.getJobForQuote(q.id)
                }
                loaded = true
            }

            if (!loaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val q = quote
                val j = job
                if (q == null || j == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Receipt not found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    ReceiptViewerScreen(
                        quote = q,
                        job = j,
                        customerName = "Customer",
                        technicianName = userName,
                        navManager = navManager
                    )
                }
            }
        }
    }
}