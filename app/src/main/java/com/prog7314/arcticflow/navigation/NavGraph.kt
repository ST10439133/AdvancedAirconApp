// app/src/main/java/com/prog7314/arcticflow/navigation/NavGraph.kt
package com.prog7314.arcticflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.prog7314.arcticflow.auth.AuthState
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.ui.screens.*
import com.prog7314.arcticflow.ui.theme.ThemeState
import com.prog7314.arcticflow.viewmodels.*

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
    }
}